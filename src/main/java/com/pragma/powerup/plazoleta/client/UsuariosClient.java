package com.pragma.powerup.plazoleta.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

@Component
public class UsuariosClient {

    private final String usuariosBaseUrl;
    private final AuthHeaderProvider authHeaderProvider;

    public UsuariosClient(
            @Value("${microservices.usuarios.url}") String usuariosBaseUrl,
            AuthHeaderProvider authHeaderProvider
    ) {
        this.usuariosBaseUrl = usuariosBaseUrl;
        this.authHeaderProvider = authHeaderProvider;
    }

    public boolean validarRolPropietario(Long idUsuario) {
        String authHeader = authHeaderProvider.getAuthorizationHeader()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no presente"));

        String url = usuariosBaseUrl + "/api/v1/usuarios/" + idUsuario + "/validacion-propietario";
        UsuarioPropietarioValidationResponse response = WebClient.create(url)
                .get()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(UsuarioPropietarioValidationResponse.class)
                .block();

        return response != null && response.propietarioValido();
    }

    public boolean validarRolEmpleado(Long idUsuario) {
        String authHeader = authHeaderProvider.getAuthorizationHeader()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token no presente"));

        String url = usuariosBaseUrl + "/api/v1/usuarios/" + idUsuario + "/validacion-empleado";
        UsuarioEmpleadoValidationResponse response = WebClient.create(url)
                .get()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(UsuarioEmpleadoValidationResponse.class)
                .block();

        return response != null && response.empleadoValido();
    }

    // Nota: el AuthHeaderProvider ya encapsula el acceso al request actual.
}
