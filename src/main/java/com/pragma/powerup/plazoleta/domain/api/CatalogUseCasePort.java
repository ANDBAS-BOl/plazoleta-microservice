package com.pragma.powerup.plazoleta.domain.api;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CatalogUseCasePort {

    Long createRestaurant(RestaurantModel restaurantModel);

    Long createDish(DishModel dishModel, Long ownerId);

    void updateDish(Long idDish, Long ownerId, DishModel dishModel);

    void setDishActive(Long idDish, Long ownerId, boolean active);

    Page<RestaurantModel> listRestaurants(Pageable pageable);

    Page<DishModel> listDishes(Long restaurantId, String categoria, Pageable pageable);
}
