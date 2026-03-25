package com.pragma.powerup.plazoleta.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class TrazabilidadClient {

    private final String trazabilidadBaseUrl;
    private final AuthHeaderProvider authHeaderProvider;

    public TrazabilidadClient(
            @Value("${microservices.trazabilidad.url}") String trazabilidadBaseUrl,
            AuthHeaderProvider authHeaderProvider
    ) {
        this.trazabilidadBaseUrl = trazabilidadBaseUrl;
        this.authHeaderProvider = authHeaderProvider;
    }

    public void registrarEvento(Map<String, Object> payload) {
        String url = trazabilidadBaseUrl + "/api/v1/trazabilidad/eventos";

        WebClient.RequestHeadersSpec<?> request = WebClient.create(url)
                .post()
                .bodyValue(payload);

        authHeaderProvider.getAuthorizationHeader()
                .ifPresent(token -> request.header(HttpHeaders.AUTHORIZATION, token));

        request.retrieve()
                .toBodilessEntity()
                .block();
    }
}

