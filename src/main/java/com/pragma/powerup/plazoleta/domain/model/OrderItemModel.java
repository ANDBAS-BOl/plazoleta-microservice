package com.pragma.powerup.plazoleta.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderItemModel {
    Long idPlato;
    String nombrePlato;
    Integer cantidad;
}
