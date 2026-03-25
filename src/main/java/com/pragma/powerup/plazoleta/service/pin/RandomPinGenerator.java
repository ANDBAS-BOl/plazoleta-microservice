package com.pragma.powerup.plazoleta.service.pin;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class RandomPinGenerator implements PinGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generarPin6Digitos() {
        // 0..999999 + left pad a 6 dígitos.
        return String.format("%06d", random.nextInt(1_000_000));
    }
}

