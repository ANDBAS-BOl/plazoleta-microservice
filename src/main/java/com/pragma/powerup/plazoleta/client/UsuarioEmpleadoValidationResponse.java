package com.pragma.powerup.plazoleta.client;

public record UsuarioEmpleadoValidationResponse(
        Long idUsuario,
        String rol,
        boolean empleadoValido
) {
}

