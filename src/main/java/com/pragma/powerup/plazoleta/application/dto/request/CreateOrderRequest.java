package com.pragma.powerup.plazoleta.application.dto.request;

import java.util.List;

public record CreateOrderRequest(
        Long idRestaurante,
        String telefonoCliente,
        List<OrderDish> platos
) {
    public record OrderDish(Long idPlato, Integer cantidad) {
    }
}
