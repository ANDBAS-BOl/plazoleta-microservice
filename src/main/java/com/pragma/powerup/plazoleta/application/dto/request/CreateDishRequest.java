package com.pragma.powerup.plazoleta.application.dto.request;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateDishRequest(
        @NotBlank(message = "El nombre del plato es obligatorio")
        String nombre,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "1", message = "El precio debe ser un numero entero positivo mayor a 0")
        BigDecimal precio,

        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,

        @NotBlank(message = "La URL de la imagen es obligatoria")
        String urlImagen,

        @NotBlank(message = "La categoria es obligatoria")
        String categoria,

        @NotNull(message = "El idRestaurante es obligatorio")
        Long idRestaurante
) {
}
