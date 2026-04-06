package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.exception.AccessDeniedException;
import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.exception.InternalProcessException;
import com.pragma.powerup.plazoleta.domain.exception.ResourceNotFoundException;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.OrderMessagingPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.OrderPinGeneratorPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceabilityPort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class OrderUseCase implements OrderUseCasePort {

    private static final int MAX_PIN_GENERATION_ATTEMPTS = 10;

    private final OrderPersistencePort orderPersistencePort;
    private final OrderTraceabilityPort orderTraceabilityPort;
    private final OrderMessagingPort orderMessagingPort;
    private final OrderPinGeneratorPort orderPinGeneratorPort;

    public OrderUseCase(OrderPersistencePort orderPersistencePort,
                        OrderTraceabilityPort orderTraceabilityPort,
                        OrderMessagingPort orderMessagingPort,
                        OrderPinGeneratorPort orderPinGeneratorPort) {
        this.orderPersistencePort = orderPersistencePort;
        this.orderTraceabilityPort = orderTraceabilityPort;
        this.orderMessagingPort = orderMessagingPort;
        this.orderPinGeneratorPort = orderPinGeneratorPort;
    }

    @Override
    public OrderModel createOrder(Long clientId, Long restaurantId, String telefonoCliente, List<OrderItemModel> items) {
        if (orderPersistencePort.hasActiveOrder(clientId)) {
            throw new BusinessRuleException("Ya tienes un pedido en proceso");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessRuleException("El pedido debe contener platos");
        }
        RestaurantModel restaurant = orderPersistencePort.findRestaurantById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante no existe"));

        for (OrderItemModel item : items) {
            DishModel dish = orderPersistencePort.findDishById(item.getIdPlato())
                    .orElseThrow(() -> new ResourceNotFoundException("Plato no existe"));
            if (!Boolean.TRUE.equals(dish.getActivo())) {
                throw new BusinessRuleException("No se pueden pedir platos inactivos");
            }
            if (!dish.getIdRestaurante().equals(restaurant.getId())) {
                throw new BusinessRuleException("Un pedido debe incluir platos de un solo restaurante");
            }
        }

        OrderModel toCreate = OrderModel.builder()
                .idRestaurante(restaurantId)
                .idCliente(clientId)
                .telefonoCliente(telefonoCliente == null ? "0000000000" : telefonoCliente)
                .estado(EstadoPedidoModel.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .items(items)
                .build();

        OrderModel saved = orderPersistencePort.saveOrder(toCreate);
        orderTraceabilityPort.registerTransition(saved, null, EstadoPedidoModel.PENDIENTE);
        return saved;
    }

    @Override
    public PageResult<OrderModel> listOrdersByStatus(Long employeeId, EstadoPedidoModel estado, PaginationParams pagination) {
        Long restaurantId = orderPersistencePort.findRestaurantIdByEmployee(employeeId)
                .orElseThrow(() -> new AccessDeniedException("El empleado no esta asignado a un restaurante"));
        return orderPersistencePort.listOrdersByStatus(restaurantId, estado, pagination);
    }

    @Override
    public OrderModel takeOrder(Long idOrder, Long employeeId) {
        OrderModel order = orderPersistencePort.findOrderById(idOrder)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no existe"));
        Long employeeRestaurantId = orderPersistencePort.findRestaurantIdByEmployee(employeeId)
                .orElseThrow(() -> new AccessDeniedException("El empleado no esta asignado a un restaurante"));

        if (!employeeRestaurantId.equals(order.getIdRestaurante())) {
            throw new AccessDeniedException("Solo puedes tomar pedidos de tu restaurante");
        }
        int updated = orderPersistencePort.takeOrderIfPending(idOrder, employeeId);
        if (updated == 0) {
            throw new BusinessRuleException("El pedido ya fue asignado o no esta pendiente");
        }
        OrderModel current = orderPersistencePort.findOrderById(idOrder)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no existe"));
        orderTraceabilityPort.registerTransition(current, EstadoPedidoModel.PENDIENTE, EstadoPedidoModel.EN_PREPARACION);
        return current;
    }

    @Override
    public OrderModel markReady(Long idOrder, Long employeeId) {
        OrderModel order = orderPersistencePort.findOrderById(idOrder)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no existe"));
        if (!employeeId.equals(order.getIdEmpleadoAsignado())) {
            throw new AccessDeniedException("Solo el empleado asignado puede marcar LISTO");
        }
        if (order.getEstado() != EstadoPedidoModel.EN_PREPARACION) {
            throw new BusinessRuleException("Solo pedidos EN_PREPARACION pueden pasar a LISTO");
        }

        String pin = generateUniquePin();
        orderMessagingPort.sendOrderReadyPin(order.getTelefonoCliente(), pin);

        OrderModel updated = OrderModel.builder()
                .id(order.getId())
                .idRestaurante(order.getIdRestaurante())
                .idCliente(order.getIdCliente())
                .telefonoCliente(order.getTelefonoCliente())
                .estado(EstadoPedidoModel.LISTO)
                .fechaCreacion(order.getFechaCreacion())
                .fechaEntrega(order.getFechaEntrega())
                .pinSeguridad(pin)
                .idEmpleadoAsignado(order.getIdEmpleadoAsignado())
                .items(order.getItems())
                .build();
        OrderModel saved = orderPersistencePort.saveOrder(updated);
        orderTraceabilityPort.registerTransition(saved, EstadoPedidoModel.EN_PREPARACION, EstadoPedidoModel.LISTO);
        return saved;
    }

    @Override
    public OrderModel deliverOrder(Long idOrder, Long employeeId, String pin) {
        OrderModel order = orderPersistencePort.findOrderById(idOrder)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no existe"));
        if (!employeeId.equals(order.getIdEmpleadoAsignado())) {
            throw new AccessDeniedException("Solo el empleado asignado puede entregar");
        }
        if (order.getEstado() != EstadoPedidoModel.LISTO) {
            throw new BusinessRuleException("Solo pedidos LISTO pueden pasar a ENTREGADO");
        }
        if (!Objects.equals(order.getPinSeguridad(), pin)) {
            throw new BusinessRuleException("PIN invalido");
        }

        OrderModel updated = OrderModel.builder()
                .id(order.getId())
                .idRestaurante(order.getIdRestaurante())
                .idCliente(order.getIdCliente())
                .telefonoCliente(order.getTelefonoCliente())
                .estado(EstadoPedidoModel.ENTREGADO)
                .fechaCreacion(order.getFechaCreacion())
                .fechaEntrega(LocalDateTime.now())
                .pinSeguridad(null)
                .idEmpleadoAsignado(order.getIdEmpleadoAsignado())
                .items(order.getItems())
                .build();
        OrderModel saved = orderPersistencePort.saveOrder(updated);
        orderTraceabilityPort.registerTransition(saved, EstadoPedidoModel.LISTO, EstadoPedidoModel.ENTREGADO);
        return saved;
    }

    @Override
    public OrderModel cancelOrder(Long idOrder, Long clientId) {
        OrderModel order = orderPersistencePort.findOrderById(idOrder)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no existe"));
        if (!order.getIdCliente().equals(clientId)) {
            throw new AccessDeniedException("No puedes cancelar pedidos de otro cliente");
        }
        if (order.getEstado() != EstadoPedidoModel.PENDIENTE) {
            throw new BusinessRuleException("Lo sentimos, tu pedido ya esta en preparacion y no puede cancelarse");
        }

        OrderModel updated = OrderModel.builder()
                .id(order.getId())
                .idRestaurante(order.getIdRestaurante())
                .idCliente(order.getIdCliente())
                .telefonoCliente(order.getTelefonoCliente())
                .estado(EstadoPedidoModel.CANCELADO)
                .fechaCreacion(order.getFechaCreacion())
                .fechaEntrega(order.getFechaEntrega())
                .pinSeguridad(order.getPinSeguridad())
                .idEmpleadoAsignado(order.getIdEmpleadoAsignado())
                .items(order.getItems())
                .build();
        OrderModel saved = orderPersistencePort.saveOrder(updated);
        orderTraceabilityPort.registerTransition(saved, EstadoPedidoModel.PENDIENTE, EstadoPedidoModel.CANCELADO);
        return saved;
    }

    private String generateUniquePin() {
        for (int attempt = 0; attempt < MAX_PIN_GENERATION_ATTEMPTS; attempt++) {
            String pin = orderPinGeneratorPort.generatePin6Digits();
            if (!orderPersistencePort.existsPinSeguridad(pin)) {
                return pin;
            }
        }
        throw new InternalProcessException("No se pudo generar un PIN unico");
    }
}
