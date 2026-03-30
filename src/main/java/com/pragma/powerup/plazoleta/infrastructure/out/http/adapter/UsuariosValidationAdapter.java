package com.pragma.powerup.plazoleta.infrastructure.out.http.adapter;

import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;

public class UsuariosValidationAdapter implements UsuariosValidationPort {

    private final UsuariosClient usuariosClient;

    public UsuariosValidationAdapter(UsuariosClient usuariosClient) {
        this.usuariosClient = usuariosClient;
    }

    @Override
    public boolean isPropietario(Long idUsuario) {
        return usuariosClient.validarRolPropietario(idUsuario);
    }
}
