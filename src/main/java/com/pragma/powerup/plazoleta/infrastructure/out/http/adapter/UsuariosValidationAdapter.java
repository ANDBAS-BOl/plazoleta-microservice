package com.pragma.powerup.plazoleta.infrastructure.out.http.adapter;

import com.pragma.powerup.plazoleta.domain.exception.InternalProcessException;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.UsuariosClient;

public class UsuariosValidationAdapter implements UsuariosValidationPort {

    private final UsuariosClient usuariosClient;

    public UsuariosValidationAdapter(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    @Override
    public boolean isPropietario(Long idUsuario) {
        try {
            return usuariosClient.validarRolPropietario(idUsuario);
        } catch (Exception e) {
            throw new InternalProcessException("Error al validar rol de propietario con Usuarios: " + e.getMessage());
        }
    }

    @Override
    public boolean isEmpleado(Long idUsuario) {
        try {
            return usuariosClient.validarRolEmpleado(idUsuario);
        } catch (Exception e) {
            throw new InternalProcessException("Error al validar rol de empleado con Usuarios: " + e.getMessage());
        }
    }
}
