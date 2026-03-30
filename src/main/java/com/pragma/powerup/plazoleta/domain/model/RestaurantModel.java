package com.pragma.powerup.plazoleta.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RestaurantModel {
    Long id;
    String nombre;
    String nit;
    String direccion;
    String telefono;
    String urlLogo;
    Long idPropietario;
}
