package com.pragma.powerup.plazoleta.application.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public record CreateRestaurantRequest(
        @NotBlank(message = "El nombre del restaurante es obligatorio")
        String nombre,

        @NotBlank(message = "El NIT es obligatorio")
        String nit,

        @NotBlank(message = "La direccion es obligatoria")
        String direccion,

        @NotBlank(message = "El telefono es obligatorio")
        String telefono,

        @NotBlank(message = "La URL del logo es obligatoria")
        String urlLogo,

        @NotNull(message = "El idPropietario es obligatorio")
        Long idPropietario
) {
}
