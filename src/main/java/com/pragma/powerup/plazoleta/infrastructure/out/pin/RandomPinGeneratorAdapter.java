package com.pragma.powerup.plazoleta.infrastructure.out.pin;

import com.pragma.powerup.plazoleta.domain.spi.OrderPinGeneratorPort;

import java.security.SecureRandom;

public class RandomPinGeneratorAdapter implements OrderPinGeneratorPort {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generatePin6Digits() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
