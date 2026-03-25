package com.pragma.powerup.plazoleta.web.dto;

import java.math.BigDecimal;

public record CreateDishRequest(
        String nombre,
        BigDecimal precio,
        String descripcion,
        String urlImagen,
        String categoria,
        Long idRestaurante
) {
}
