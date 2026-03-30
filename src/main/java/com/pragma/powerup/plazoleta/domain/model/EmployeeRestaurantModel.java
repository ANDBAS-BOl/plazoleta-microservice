package com.pragma.powerup.plazoleta.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmployeeRestaurantModel {
    Long id;
    Long idEmpleado;
    Long idRestaurante;
}
