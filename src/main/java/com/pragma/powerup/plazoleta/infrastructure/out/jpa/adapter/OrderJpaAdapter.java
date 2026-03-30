package com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter;

import com.pragma.powerup.plazoleta.domain.*;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.OrderPersistencePort;
import com.pragma.powerup.plazoleta.repository.DishRepository;
import com.pragma.powerup.plazoleta.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.repository.OrderRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderJpaAdapter implements OrderPersistencePort {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRestaurantRepository employeeRestaurantRepository;

    public OrderJpaAdapter(RestaurantRepository restaurantRepository,
                           DishRepository dishRepository,
                           OrderRepository orderRepository,
                           EmployeeRestaurantRepository employeeRestaurantRepository) {
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
        this.orderRepository = orderRepository;
        this.employeeRestaurantRepository = employeeRestaurantRepository;
    }

    @Override
    public boolean hasActiveOrder(Long clientId) {
        return orderRepository.existsByIdClienteAndEstadoIn(
                clientId,
                List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO));
    }

    @Override
    public Optional<RestaurantModel> findRestaurantById(Long idRestaurante) {
        return restaurantRepository.findById(idRestaurante).map(this::toRestaurantModel);
    }

    @Override
    public Optional<DishModel> findDishById(Long idPlato) {
        return dishRepository.findById(idPlato).map(this::toDishModel);
    }

    @Override
    public OrderModel saveOrder(OrderModel orderModel) {
        boolean isNew = orderModel.getId() == null;
        OrderEntity entity = isNew
                ? new OrderEntity()
                : orderRepository.findById(orderModel.getId()).orElseThrow();

        if (isNew) {
            RestaurantEntity restaurant = restaurantRepository.findById(orderModel.getIdRestaurante())
                    .orElseThrow();
            entity.setRestaurant(restaurant);
        } else if (!entity.getRestaurant().getId().equals(orderModel.getIdRestaurante())) {
            RestaurantEntity restaurant = restaurantRepository.findById(orderModel.getIdRestaurante())
                    .orElseThrow();
            entity.setRestaurant(restaurant);
        }

        entity.setIdCliente(orderModel.getIdCliente());
        entity.setTelefonoCliente(orderModel.getTelefonoCliente());
        entity.setEstado(toEstadoEntity(orderModel.getEstado()));
        entity.setFechaCreacion(orderModel.getFechaCreacion());
        entity.setFechaEntrega(orderModel.getFechaEntrega());
        entity.setPinSeguridad(orderModel.getPinSeguridad());
        entity.setIdEmpleadoAsignado(orderModel.getIdEmpleadoAsignado());

        if (orderModel.getItems() != null) {
            entity.getItems().clear();
            for (OrderItemModel itemModel : orderModel.getItems()) {
                OrderItemEntity itemEntity = new OrderItemEntity();
                itemEntity.setOrder(entity);
                itemEntity.setDish(dishRepository.findById(itemModel.getIdPlato()).orElseThrow());
                itemEntity.setCantidad(itemModel.getCantidad());
                entity.getItems().add(itemEntity);
            }
        }

        if (isNew) {
            return toOrderModel(orderRepository.save(entity));
        }
        return toOrderModel(entity);
    }

    @Override
    public Page<OrderModel> listOrdersByStatus(Long idRestaurante, EstadoPedidoModel estado, Pageable pageable) {
        return orderRepository.findByRestaurantIdAndEstado(idRestaurante, toEstadoEntity(estado), pageable)
                .map(this::toOrderModel);
    }

    @Override
    public Optional<Long> findRestaurantIdByEmployee(Long employeeId) {
        return employeeRestaurantRepository.findFirstByIdEmpleado(employeeId)
                .map(er -> er.getRestaurant().getId());
    }

    @Override
    public Optional<OrderModel> findOrderById(Long idOrder) {
        return orderRepository.findById(idOrder).map(this::toOrderModel);
    }

    @Override
    public int takeOrderIfPending(Long idOrder, Long employeeId) {
        return orderRepository.takeOrderIfPending(
                idOrder,
                EstadoPedido.PENDIENTE,
                EstadoPedido.EN_PREPARACION,
                employeeId);
    }

    @Override
    public boolean existsPinSeguridad(String pin) {
        return orderRepository.existsByPinSeguridad(pin);
    }

    private EstadoPedido toEstadoEntity(EstadoPedidoModel model) {
        return EstadoPedido.valueOf(model.name());
    }

    private EstadoPedidoModel toEstadoModel(EstadoPedido estadoPedido) {
        return EstadoPedidoModel.valueOf(estadoPedido.name());
    }

    private RestaurantModel toRestaurantModel(RestaurantEntity entity) {
        return RestaurantModel.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .nit(entity.getNit())
                .direccion(entity.getDireccion())
                .telefono(entity.getTelefono())
                .urlLogo(entity.getUrlLogo())
                .idPropietario(entity.getIdPropietario())
                .build();
    }

    private DishModel toDishModel(DishEntity entity) {
        return DishModel.builder()
                .id(entity.getId())
                .idRestaurante(entity.getRestaurant().getId())
                .nombre(entity.getNombre())
                .precio(entity.getPrecio())
                .descripcion(entity.getDescripcion())
                .urlImagen(entity.getUrlImagen())
                .categoria(entity.getCategoria())
                .activo(entity.getActivo())
                .build();
    }

    private OrderModel toOrderModel(OrderEntity entity) {
        List<OrderItemModel> items = entity.getItems() == null
                ? new ArrayList<>()
                : entity.getItems().stream()
                .map(i -> OrderItemModel.builder()
                        .idPlato(i.getDish().getId())
                        .nombrePlato(i.getDish().getNombre())
                        .cantidad(i.getCantidad())
                        .build())
                .collect(Collectors.toList());

        return OrderModel.builder()
                .id(entity.getId())
                .idRestaurante(entity.getRestaurant().getId())
                .idCliente(entity.getIdCliente())
                .telefonoCliente(entity.getTelefonoCliente())
                .estado(toEstadoModel(entity.getEstado()))
                .fechaCreacion(entity.getFechaCreacion())
                .fechaEntrega(entity.getFechaEntrega())
                .pinSeguridad(entity.getPinSeguridad())
                .idEmpleadoAsignado(entity.getIdEmpleadoAsignado())
                .items(items)
                .build();
    }
}
