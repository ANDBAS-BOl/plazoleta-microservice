package com.pragma.powerup.plazoleta.domain.spi;

public interface OrderMessagingPort {

    void sendOrderReadyPin(String phoneNumber, String pin);
}
