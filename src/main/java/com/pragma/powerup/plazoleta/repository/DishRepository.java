package com.pragma.powerup.plazoleta.repository;

import com.pragma.powerup.plazoleta.domain.DishEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishRepository extends JpaRepository<DishEntity, Long> {
    Page<DishEntity> findByRestaurantIdAndActivoTrue(Long restaurantId, Pageable pageable);

    Page<DishEntity> findByRestaurantIdAndActivoTrueAndCategoriaIgnoreCase(Long restaurantId, String categoria, Pageable pageable);
}
