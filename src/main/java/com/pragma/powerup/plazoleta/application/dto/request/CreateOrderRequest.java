package com.pragma.powerup.plazoleta.application.dto.request;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "El idRestaurante es obligatorio")
        Long idRestaurante,

        String telefonoCliente,

        @NotEmpty(message = "El pedido debe contener al menos un plato")
        @Valid
        List<OrderDish> platos
) {
    public record OrderDish(
            @NotNull(message = "El idPlato es obligatorio")
            Long idPlato,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
            Integer cantidad
    ) {
    }
}
