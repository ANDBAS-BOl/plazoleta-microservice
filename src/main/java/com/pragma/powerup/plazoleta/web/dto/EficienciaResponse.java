package com.pragma.powerup.plazoleta.web.dto;

import java.util.List;

public record EficienciaResponse(List<EficienciaEmpleado> ranking) {
    public record EficienciaEmpleado(Long idEmpleadoAsignado, double tiempoPromedioMinutos) {
    }
}
