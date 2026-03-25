package com.pragma.powerup.plazoleta.client;

import java.util.Optional;

public interface AuthHeaderProvider {
    Optional<String> getAuthorizationHeader();
}

