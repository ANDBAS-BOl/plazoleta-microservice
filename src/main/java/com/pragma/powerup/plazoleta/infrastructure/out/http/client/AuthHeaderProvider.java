package com.pragma.powerup.plazoleta.infrastructure.out.http.client;

import java.util.Optional;

public interface AuthHeaderProvider {
    Optional<String> getAuthorizationHeader();
}
