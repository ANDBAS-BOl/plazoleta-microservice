package com.pragma.powerup.plazoleta.application.handler;

import com.pragma.powerup.plazoleta.application.dto.request.AssignEmployeeRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.application.dto.request.UpdateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.response.DishResponse;
import com.pragma.powerup.plazoleta.application.dto.response.RestaurantCardResponse;
import com.pragma.powerup.plazoleta.domain.model.PageResult;

public interface ICatalogHandler {

    Long createRestaurant(CreateRestaurantRequest request);

    Long createDish(CreateDishRequest request, Long ownerId);

    Long assignEmployeeToRestaurant(Long idRestaurante, AssignEmployeeRequest request, Long ownerId);

    void updateDish(Long idPlato, UpdateDishRequest request, Long ownerId);

    void changeDishStatus(Long idPlato, boolean activo, Long ownerId);

    PageResult<RestaurantCardResponse> listRestaurants(int page, int size);

    PageResult<DishResponse> listDishes(Long idRestaurante, String categoria, int page, int size);
}
