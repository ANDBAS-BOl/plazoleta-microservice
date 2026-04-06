package com.pragma.powerup.plazoleta.domain.spi;

import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;

import java.util.Optional;

public interface OrderPersistencePort {

    boolean hasActiveOrder(Long clientId);

    Optional<RestaurantModel> findRestaurantById(Long idRestaurante);

    Optional<DishModel> findDishById(Long idPlato);

    OrderModel saveOrder(OrderModel orderModel);

    PageResult<OrderModel> listOrdersByStatus(Long idRestaurante, EstadoPedidoModel estado, PaginationParams pagination);

    Optional<Long> findRestaurantIdByEmployee(Long employeeId);

    Optional<OrderModel> findOrderById(Long idOrder);

    int takeOrderIfPending(Long idOrder, Long employeeId);

    boolean existsPinSeguridad(String pin);
}
