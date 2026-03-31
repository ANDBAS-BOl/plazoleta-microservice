package com.pragma.powerup.plazoleta.domain.api;

import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;

public interface OrderEfficiencyUseCasePort {

    OrderEfficiencyModel getOwnerEfficiency(Long ownerId);
}
