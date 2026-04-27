package com.pragma.powerup.plazoleta.application.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public record DeliverOrderRequest(
        @NotBlank(message = "El PIN es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El PIN debe ser exactamente 6 digitos numericos")
        String pin
) {
}
