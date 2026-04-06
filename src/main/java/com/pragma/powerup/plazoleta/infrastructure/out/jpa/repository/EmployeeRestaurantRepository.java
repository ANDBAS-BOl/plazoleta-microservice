package com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository;

import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.EmployeeRestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRestaurantRepository extends JpaRepository<EmployeeRestaurantEntity, Long> {
    Optional<EmployeeRestaurantEntity> findFirstByIdEmpleado(Long idEmpleado);

    boolean existsByIdEmpleadoAndRestaurant_Id(Long idEmpleado, Long restaurantId);
}
