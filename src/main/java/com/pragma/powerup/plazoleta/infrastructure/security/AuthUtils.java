package com.pragma.powerup.plazoleta.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {

    public UsuarioPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioPrincipal)) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        return (UsuarioPrincipal) authentication.getPrincipal();
    }
}
