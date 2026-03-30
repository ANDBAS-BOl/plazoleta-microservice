package com.pragma.powerup.plazoleta.infrastructure.out.http.adapter;

import com.pragma.powerup.plazoleta.client.MensajeriaClient;
import com.pragma.powerup.plazoleta.domain.spi.OrderMessagingPort;

public class OrderMessagingAdapter implements OrderMessagingPort {

    private final MensajeriaClient mensajeriaClient;

    public OrderMessagingAdapter(MensajeriaClient mensajeriaClient) {
        this.mensajeriaClient = mensajeriaClient;
    }

    @Override
    public void sendOrderReadyPin(String phoneNumber, String pin) {
        mensajeriaClient.enviarSms(phoneNumber, "Tu pedido esta listo. PIN: " + pin);
    }
}
