package com.pragma.powerup.plazoleta.domain.model;

import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.utils.DomainErrorMessage;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RestaurantModel {

    static final String ONLY_DIGITS_REGEX = "\\d+";
    static final String VALID_PHONE_REGEX = "^\\+?\\d{1,13}$";
    static final String NAME_NOT_ONLY_DIGITS_REGEX = "^\\d+$";

    Long id;
    String nombre;
    String nit;
    String direccion;
    String telefono;
    String urlLogo;
    Long idPropietario;

    public static class RestaurantModelBuilder {
        public RestaurantModel build() {
            if (nit != null && !nit.matches(ONLY_DIGITS_REGEX)) {
                throw new BusinessRuleException(DomainErrorMessage.NIT_NOT_NUMERIC.getMessage());
            }
            if (telefono != null && !telefono.matches(VALID_PHONE_REGEX)) {
                throw new BusinessRuleException(DomainErrorMessage.PHONE_INVALID.getMessage());
            }
            if (nombre != null && nombre.matches(NAME_NOT_ONLY_DIGITS_REGEX)) {
                throw new BusinessRuleException(DomainErrorMessage.RESTAURANT_NAME_INVALID.getMessage());
            }
            return new RestaurantModel(id, nombre, nit, direccion, telefono, urlLogo, idPropietario);
        }
    }
}
