package com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.OrderPersistencePort;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.EstadoPedido;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderItemEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper.IDishEntityMapper;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper.IOrderEntityMapper;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper.IRestaurantEntityMapper;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.DishRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.OrderRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.RestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OrderJpaAdapter implements OrderPersistencePort {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRestaurantRepository employeeRestaurantRepository;
    private final IRestaurantEntityMapper restaurantEntityMapper;
    private final IDishEntityMapper dishEntityMapper;
    private final IOrderEntityMapper orderEntityMapper;

    public OrderJpaAdapter(RestaurantRepository restaurantRepository,
                           DishRepository dishRepository,
                           OrderRepository orderRepository,
                           EmployeeRestaurantRepository employeeRestaurantRepository,
                           IRestaurantEntityMapper restaurantEntityMapper,
                           IDishEntityMapper dishEntityMapper,
                           IOrderEntityMapper orderEntityMapper) {
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
        this.orderRepository = orderRepository;
        this.employeeRestaurantRepository = employeeRestaurantRepository;
        this.restaurantEntityMapper = restaurantEntityMapper;
        this.dishEntityMapper = dishEntityMapper;
        this.orderEntityMapper = orderEntityMapper;
    }

    @Override
    public boolean hasActiveOrder(Long clientId) {
        return orderRepository.existsByIdClienteAndEstadoIn(
                clientId,
                List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO));
    }

    @Override
    public Optional<RestaurantModel> findRestaurantById(Long idRestaurante) {
        return restaurantRepository.findById(idRestaurante).map(restaurantEntityMapper::toRestaurantModel);
    }

    @Override
    public Optional<DishModel> findDishById(Long idPlato) {
        return dishRepository.findById(idPlato).map(dishEntityMapper::toDishModel);
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
            return orderEntityMapper.toOrderModel(orderRepository.save(entity));
        }
        return orderEntityMapper.toOrderModel(entity);
    }

    @Override
    public PageResult<OrderModel> listOrdersByStatus(Long idRestaurante, EstadoPedidoModel estado, PaginationParams pagination) {
        Page<OrderEntity> page = orderRepository.findByRestaurantIdAndEstado(
                idRestaurante,
                toEstadoEntity(estado),
                PageRequest.of(pagination.page(), pagination.size()));
        return toPageResult(page);
    }

    @Override
    public Optional<Long> findRestaurantIdByEmployee(Long employeeId) {
        return employeeRestaurantRepository.findFirstByIdEmpleado(employeeId)
                .map(er -> er.getRestaurant().getId());
    }

    @Override
    public Optional<OrderModel> findOrderById(Long idOrder) {
        return orderRepository.findById(idOrder).map(orderEntityMapper::toOrderModel);
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

    private PageResult<OrderModel> toPageResult(Page<OrderEntity> page) {
        List<OrderModel> content = page.getContent().stream()
                .map(orderEntityMapper::toOrderModel)
                .collect(Collectors.toList());
        return new PageResult<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private EstadoPedido toEstadoEntity(EstadoPedidoModel model) {
        return EstadoPedido.valueOf(model.name());
    }
}
