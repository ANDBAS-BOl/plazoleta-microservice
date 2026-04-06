package com.pragma.powerup.plazoleta.infrastructure.out.http.adapter;

import com.pragma.powerup.plazoleta.domain.exception.InternalProcessException;
import com.pragma.powerup.plazoleta.domain.spi.OrderMessagingPort;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.MensajeriaClient;

public class OrderMessagingAdapter implements OrderMessagingPort {

    private final MensajeriaClient mensajeriaClient;

    public OrderMessagingAdapter(MensajeriaClient mensajeriaClient) {
        this.mensajeriaClient = mensajeriaClient;
    }

    @Override
    public void sendOrderReadyPin(String phoneNumber, String pin) {
        try {
            mensajeriaClient.enviarSms(phoneNumber, "Tu pedido esta listo. PIN: " + pin);
        } catch (Exception e) {
            throw new InternalProcessException("Error al enviar SMS de pedido listo: " + e.getMessage());
        }
    }
}
