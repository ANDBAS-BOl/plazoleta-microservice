package com.pragma.powerup.plazoleta.domain.spi;

public interface UsuariosValidationPort {

    boolean isPropietario(Long idUsuario);

    boolean isEmpleado(Long idUsuario);
}
