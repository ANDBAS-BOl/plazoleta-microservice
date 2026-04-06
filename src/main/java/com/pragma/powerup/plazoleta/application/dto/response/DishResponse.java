package com.pragma.powerup.plazoleta.application.dto.response;

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
