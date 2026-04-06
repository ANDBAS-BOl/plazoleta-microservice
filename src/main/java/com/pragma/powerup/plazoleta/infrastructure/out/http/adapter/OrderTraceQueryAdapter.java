package com.pragma.powerup.plazoleta.infrastructure.out.http.adapter;

import com.pragma.powerup.plazoleta.domain.exception.InternalProcessException;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceQueryPort;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.TrazabilidadClient;

public class OrderTraceQueryAdapter implements OrderTraceQueryPort {

    private final TrazabilidadClient trazabilidadClient;

    public OrderTraceQueryAdapter(TrazabilidadClient trazabilidadClient) {
        this.trazabilidadClient = trazabilidadClient;
    }

    @Override
    public Object getTraceByOrderId(Long idOrder) {
        try {
            return trazabilidadClient.obtenerTrazabilidadPedido(idOrder);
        } catch (Exception e) {
            throw new InternalProcessException("Error al consultar trazabilidad del pedido: " + e.getMessage());
        }
    }
}
