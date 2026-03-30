package com.pragma.powerup.plazoleta.domain.spi;

import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderPersistencePort {

    boolean hasActiveOrder(Long clientId);

    Optional<RestaurantModel> findRestaurantById(Long idRestaurante);

    Optional<DishModel> findDishById(Long idPlato);

    OrderModel saveOrder(OrderModel orderModel);

    Page<OrderModel> listOrdersByStatus(Long idRestaurante, EstadoPedidoModel estado, Pageable pageable);

    Optional<Long> findRestaurantIdByEmployee(Long employeeId);

    Optional<OrderModel> findOrderById(Long idOrder);

    int takeOrderIfPending(Long idOrder, Long employeeId);

    boolean existsPinSeguridad(String pin);
}
