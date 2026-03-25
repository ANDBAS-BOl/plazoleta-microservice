package com.pragma.powerup.plazoleta.web.dto;

import java.math.BigDecimal;

public record DishResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        String descripcion,
        String urlImagen,
        String categoria,
        Boolean activo
) {
}
