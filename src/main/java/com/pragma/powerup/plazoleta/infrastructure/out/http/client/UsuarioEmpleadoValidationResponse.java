package com.pragma.powerup.plazoleta.infrastructure.out.http.client;

public record UsuarioEmpleadoValidationResponse(
        Long idUsuario,
        String rol,
        boolean empleadoValido
) {
}
