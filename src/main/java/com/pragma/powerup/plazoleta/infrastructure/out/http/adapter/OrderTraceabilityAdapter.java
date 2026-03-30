package com.pragma.powerup.plazoleta.infrastructure.out.http.adapter;

import com.pragma.powerup.plazoleta.client.TrazabilidadClient;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceabilityPort;

import java.util.HashMap;
import java.util.Map;

public class OrderTraceabilityAdapter implements OrderTraceabilityPort {

    private final TrazabilidadClient trazabilidadClient;

    public OrderTraceabilityAdapter(TrazabilidadClient trazabilidadClient) {
        this.trazabilidadClient = trazabilidadClient;
    }

    @Override
    public void registerTransition(OrderModel orderModel, EstadoPedidoModel from, EstadoPedidoModel to) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("idPedido", orderModel.getId());
        payload.put("idCliente", orderModel.getIdCliente());
        payload.put("idRestaurante", orderModel.getIdRestaurante());
        payload.put("idEmpleado", orderModel.getIdEmpleadoAsignado());
        payload.put("estadoAnterior", from == null ? null : from.name());
        payload.put("estadoNuevo", to.name());
        try {
            trazabilidadClient.registrarEvento(payload);
        } catch (Exception ignored) {
            // Se preserva la semantica actual: falla de trazabilidad no bloquea flujo de negocio.
        }
    }
}
