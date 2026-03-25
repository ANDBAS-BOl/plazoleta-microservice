package com.pragma.powerup.plazoleta.repository;

import com.pragma.powerup.plazoleta.domain.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
}
