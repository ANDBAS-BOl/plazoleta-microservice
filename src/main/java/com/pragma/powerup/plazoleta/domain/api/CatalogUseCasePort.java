package com.pragma.powerup.plazoleta.domain.api;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;

public interface CatalogUseCasePort {

    Long createRestaurant(RestaurantModel restaurantModel);

    Long createDish(DishModel dishModel, Long ownerId);

    void updateDish(Long idDish, Long ownerId, DishModel dishModel);

    void setDishActive(Long idDish, Long ownerId, boolean active);

    PageResult<RestaurantModel> listRestaurants(PaginationParams pagination);

    PageResult<DishModel> listDishes(Long restaurantId, String categoria, PaginationParams pagination);

    Long assignEmployeeToRestaurant(Long restaurantId, Long employeeId, Long ownerId);
}
