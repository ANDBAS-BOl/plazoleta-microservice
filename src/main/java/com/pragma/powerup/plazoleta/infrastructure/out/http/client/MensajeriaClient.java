package com.pragma.powerup.plazoleta.infrastructure.out.http.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class MensajeriaClient {

    private final String mensajeriaBaseUrl;
    private final AuthHeaderProvider authHeaderProvider;

    public MensajeriaClient(
            @Value("${microservices.mensajeria.url}") String mensajeriaBaseUrl,
            AuthHeaderProvider authHeaderProvider
    ) {
        this.mensajeriaBaseUrl = mensajeriaBaseUrl;
        this.authHeaderProvider = authHeaderProvider;
    }

    public void enviarSms(String phoneNumber, String message) {
        String url = mensajeriaBaseUrl + "/api/v1/mensajeria/sms";
        Map<String, String> payload = Map.of("phoneNumber", phoneNumber, "message", message);

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
