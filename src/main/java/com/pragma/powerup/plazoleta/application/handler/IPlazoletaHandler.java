package com.pragma.powerup.plazoleta.application.handler;

import com.pragma.powerup.plazoleta.domain.EstadoPedido;
import com.pragma.powerup.plazoleta.web.dto.AssignEmployeeRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateDishRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateOrderRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.web.dto.DeliverOrderRequest;
import com.pragma.powerup.plazoleta.web.dto.DishResponse;
import com.pragma.powerup.plazoleta.web.dto.EficienciaResponse;
import com.pragma.powerup.plazoleta.web.dto.OrderResponse;
import com.pragma.powerup.plazoleta.web.dto.RestaurantCardResponse;
import com.pragma.powerup.plazoleta.web.dto.UpdateDishRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPlazoletaHandler {
    Long createRestaurant(CreateRestaurantRequest request);

    Long createDish(CreateDishRequest request);

    Long assignEmployeeToRestaurant(Long idRestaurante, AssignEmployeeRequest request);

    void updateDish(Long idPlato, UpdateDishRequest request);

    void changeDishStatus(Long idPlato, boolean activo);

    Page<RestaurantCardResponse> listRestaurants(Pageable pageable);

    Page<DishResponse> listDishes(Long idRestaurante, String categoria, Pageable pageable);

    OrderResponse createOrder(CreateOrderRequest request);

    Page<OrderResponse> listOrdersByStatus(EstadoPedido estado, Pageable pageable);

    OrderResponse takeOrder(Long idPedido);

    OrderResponse markReady(Long idPedido);

    OrderResponse deliverOrder(Long idPedido, DeliverOrderRequest request);

    OrderResponse cancelOrder(Long idPedido);

    Object trace(Long idPedido);

    EficienciaResponse efficiency();
}
