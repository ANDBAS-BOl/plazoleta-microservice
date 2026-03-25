package com.pragma.powerup.plazoleta.web.dto;

import java.math.BigDecimal;

public record UpdateDishRequest(
        BigDecimal precio,
        String descripcion
) {
}
