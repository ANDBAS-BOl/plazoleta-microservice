package com.pragma.powerup.plazoleta.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class DishModel {
    Long id;
    Long idRestaurante;
    String nombre;
    BigDecimal precio;
    String descripcion;
    String urlImagen;
    String categoria;
    Boolean activo;
}
