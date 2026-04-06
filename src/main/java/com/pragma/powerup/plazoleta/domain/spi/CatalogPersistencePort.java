package com.pragma.powerup.plazoleta.domain.spi;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;

import java.util.Optional;

public interface CatalogPersistencePort {

    Long createRestaurant(RestaurantModel restaurantModel);

    Optional<RestaurantModel> findRestaurantById(Long id);

    Long createDish(DishModel dishModel);

    Optional<DishModel> findDishById(Long id);

    void updateDish(DishModel dishModel);

    PageResult<RestaurantModel> listRestaurants(PaginationParams pagination);

    PageResult<DishModel> listActiveDishes(Long restaurantId, String categoria, PaginationParams pagination);

    boolean existsEmployeeAssignment(Long employeeId, Long restaurantId);

    Long saveEmployeeAssignment(Long employeeId, Long restaurantId);
}
