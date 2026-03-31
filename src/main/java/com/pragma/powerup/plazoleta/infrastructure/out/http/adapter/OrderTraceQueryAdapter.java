package com.pragma.powerup.plazoleta.infrastructure.out.http.adapter;

import com.pragma.powerup.plazoleta.client.TrazabilidadClient;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceQueryPort;

public class OrderTraceQueryAdapter implements OrderTraceQueryPort {

    private final TrazabilidadClient trazabilidadClient;

    public OrderTraceQueryAdapter(TrazabilidadClient trazabilidadClient) {
        this.trazabilidadClient = trazabilidadClient;
    }

    @Override
    public Object getTraceByOrderId(Long idOrder) {
        return trazabilidadClient.obtenerTrazabilidadPedido(idOrder);
    }
}
