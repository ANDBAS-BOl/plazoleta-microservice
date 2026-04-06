package com.pragma.powerup.plazoleta.application.dto.request;

public record CreateRestaurantRequest(
        String nombre,
        String nit,
        String direccion,
        String telefono,
        String urlLogo,
        Long idPropietario
) {
}
