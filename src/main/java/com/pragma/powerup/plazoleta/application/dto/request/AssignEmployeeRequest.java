package com.pragma.powerup.plazoleta.application.dto.request;

import javax.validation.constraints.NotNull;

public record AssignEmployeeRequest(
        @NotNull(message = "El idEmpleado es obligatorio")
        Long idEmpleado
) {
}
