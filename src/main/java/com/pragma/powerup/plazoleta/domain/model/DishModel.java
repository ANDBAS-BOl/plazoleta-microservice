package com.pragma.powerup.plazoleta.domain.model;

import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.utils.DomainErrorMessage;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class DishModel {

    Long id;
    Long idRestaurante;
    String nombre;
    BigDecimal precio;
    String descripcion;
    String urlImagen;
    String categoria;
    Boolean activo;

    public void assertIsActive() {
        if (!Boolean.TRUE.equals(activo)) {
            throw new BusinessRuleException(DomainErrorMessage.DISH_INACTIVE.getMessage());
        }
    }

    public void assertBelongsToRestaurant(Long restaurantId) {
        if (!idRestaurante.equals(restaurantId)) {
            throw new BusinessRuleException(DomainErrorMessage.DISH_DIFFERENT_RESTAURANT.getMessage());
        }
    }

    public static class DishModelBuilder {
        public DishModel build() {
            if (precio != null && precio.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException(DomainErrorMessage.PRICE_NOT_POSITIVE.getMessage());
            }
            return new DishModel(id, idRestaurante, nombre, precio, descripcion, urlImagen, categoria, activo);
        }
    }
}
