package com.pragma.powerup.plazoleta.domain.model;

import com.pragma.powerup.plazoleta.domain.exception.AccessDeniedException;
import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.utils.DomainErrorMessage;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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

    public void assertBelongsToClient(Long clientId) {
        if (!idCliente.equals(clientId)) {
            throw new AccessDeniedException(DomainErrorMessage.NOT_ORDER_CLIENT.getMessage());
        }
    }

    public void assertIsPending() {
        if (estado != EstadoPedidoModel.PENDIENTE) {
            throw new BusinessRuleException(DomainErrorMessage.ORDER_NOT_CANCELABLE.getMessage());
        }
    }

    public void assertIsEnPreparacion() {
        if (estado != EstadoPedidoModel.EN_PREPARACION) {
            throw new BusinessRuleException(DomainErrorMessage.ORDER_NOT_IN_PREPARACION.getMessage());
        }
    }

    public void assertIsListo() {
        if (estado != EstadoPedidoModel.LISTO) {
            throw new BusinessRuleException(DomainErrorMessage.ORDER_NOT_LISTO.getMessage());
        }
    }

    public void assertAssignedEmployeeCanMarkReady(Long employeeId) {
        if (!employeeId.equals(idEmpleadoAsignado)) {
            throw new AccessDeniedException(DomainErrorMessage.NOT_ASSIGNED_EMPLOYEE_READY.getMessage());
        }
    }

    public void assertAssignedEmployeeCanDeliver(Long employeeId) {
        if (!employeeId.equals(idEmpleadoAsignado)) {
            throw new AccessDeniedException(DomainErrorMessage.NOT_ASSIGNED_EMPLOYEE_DELIVER.getMessage());
        }
    }

    public void assertPin(String pin) {
        if (!Objects.equals(pinSeguridad, pin)) {
            throw new BusinessRuleException(DomainErrorMessage.PIN_INVALID.getMessage());
        }
    }
}
