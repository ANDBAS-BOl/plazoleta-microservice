package com.pragma.powerup.plazoleta.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class OrderModel {
    Long id;
    Long idRestaurante;
    Long idCliente;
    String telefonoCliente;
    EstadoPedidoModel estado;
    LocalDateTime fechaCreacion;
    LocalDateTime fechaEntrega;
    String pinSeguridad;
    Long idEmpleadoAsignado;
    List<OrderItemModel> items;
}
