package com.pragma.powerup.plazoleta.domain.spi;

import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;

public interface OrderTraceabilityPort {

    void registerTransition(OrderModel orderModel, EstadoPedidoModel from, EstadoPedidoModel to);
}
