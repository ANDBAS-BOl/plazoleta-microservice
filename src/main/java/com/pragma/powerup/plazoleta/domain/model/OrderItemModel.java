package com.pragma.powerup.plazoleta.domain.model;

import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.utils.DomainErrorMessage;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OrderItemModel {

    Long idPlato;
    String nombrePlato;
    Integer cantidad;

    public static void assertNotEmpty(List<OrderItemModel> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessRuleException(DomainErrorMessage.ORDER_WITHOUT_ITEMS.getMessage());
        }
    }
}
