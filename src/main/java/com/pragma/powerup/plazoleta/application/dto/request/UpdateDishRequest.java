package com.pragma.powerup.plazoleta.application.dto.request;

import java.math.BigDecimal;

public record UpdateDishRequest(
        BigDecimal precio,
        String descripcion
) {
}
