package com.pragma.powerup.plazoleta.domain.spi;

import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;

public interface OrderEfficiencyPort {

    OrderEfficiencyModel calculateOwnerEfficiency(Long ownerId);
}
