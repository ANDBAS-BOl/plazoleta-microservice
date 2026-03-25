package com.pragma.powerup.plazoleta.web.dto;

public record CreateRestaurantRequest(
        String nombre,
        String nit,
        String direccion,
        String telefono,
        String urlLogo,
        Long idPropietario
) {
}
