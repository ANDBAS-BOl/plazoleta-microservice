package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceQueryPort;

public class OrderTraceQueryUseCase implements OrderTraceQueryUseCasePort {

    private final OrderTraceQueryPort orderTraceQueryPort;

    public OrderTraceQueryUseCase(OrderTraceQueryPort orderTraceQueryPort) {
        this.orderTraceQueryPort = orderTraceQueryPort;
    }

    @Override
    public Object getTraceByOrderId(Long idOrder) {
        return orderTraceQueryPort.getTraceByOrderId(idOrder);
    }
}
