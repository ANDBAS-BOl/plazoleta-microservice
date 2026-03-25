package com.pragma.powerup.plazoleta.client;

public record UsuarioPropietarioValidationResponse(
        Long idUsuario,
        String rol,
        boolean propietarioValido
) {
}
