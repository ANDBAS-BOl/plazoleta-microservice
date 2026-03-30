package com.pragma.powerup.plazoleta.domain.spi;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CatalogPersistencePort {

    Long createRestaurant(RestaurantModel restaurantModel);

    Optional<RestaurantModel> findRestaurantById(Long id);

    Long createDish(DishModel dishModel);

    Optional<DishModel> findDishById(Long id);

    void updateDish(DishModel dishModel);

    Page<RestaurantModel> listRestaurants(Pageable pageable);

    Page<DishModel> listActiveDishes(Long restaurantId, String categoria, Pageable pageable);
}
