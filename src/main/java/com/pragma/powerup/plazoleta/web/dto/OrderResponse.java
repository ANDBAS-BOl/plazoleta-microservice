package com.pragma.powerup.plazoleta.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long idPedido,
        Long idRestaurante,
        Long idCliente,
        List<OrderItemResponse> lineas,
        String estadoActual,
        LocalDateTime fechaCreacion,
        Long idEmpleadoAsignado
) {
    public record OrderItemResponse(Long idPlato, String nombrePlato, Integer cantidad) {
    }
}
