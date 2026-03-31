package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;
import com.pragma.powerup.plazoleta.domain.spi.OrderEfficiencyPort;

public class OrderEfficiencyUseCase implements OrderEfficiencyUseCasePort {

    private final OrderEfficiencyPort orderEfficiencyPort;

    public OrderEfficiencyUseCase(OrderEfficiencyPort orderEfficiencyPort) {
        this.orderEfficiencyPort = orderEfficiencyPort;
    }

    @Override
    public OrderEfficiencyModel getOwnerEfficiency(Long ownerId) {
        return orderEfficiencyPort.calculateOwnerEfficiency(ownerId);
    }
}
