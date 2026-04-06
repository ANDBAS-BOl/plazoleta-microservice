package com.pragma.powerup.plazoleta.infrastructure.out.http.client;

public record UsuarioPropietarioValidationResponse(
        Long idUsuario,
        String rol,
        boolean propietarioValido
) {
}
