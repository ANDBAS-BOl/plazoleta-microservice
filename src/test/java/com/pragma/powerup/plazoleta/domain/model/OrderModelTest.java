package com.pragma.powerup.plazoleta.domain.model;

import com.pragma.powerup.plazoleta.domain.exception.AccessDeniedException;
import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.utils.DomainErrorMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderModelTest {

    private OrderModel orderWith(EstadoPedidoModel estado) {
        return OrderModel.builder()
                .id(1L).idRestaurante(1L).idCliente(10L)
                .estado(estado).fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
    }

    private OrderModel orderWithEmployee(EstadoPedidoModel estado, Long employeeId) {
        return OrderModel.builder()
                .id(1L).idRestaurante(1L).idCliente(10L)
                .idEmpleadoAsignado(employeeId)
                .estado(estado).fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
    }

    // ── assertBelongsToClient ──────────────────────────────────────────────────

    @Test
    void assertBelongsToClientShouldPassForOwner() {
        OrderModel order = orderWith(EstadoPedidoModel.PENDIENTE);
        assertDoesNotThrow(() -> order.assertBelongsToClient(10L));
    }

    @Test
    void assertBelongsToClientShouldFailForOtherClient() {
        OrderModel order = orderWith(EstadoPedidoModel.PENDIENTE);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> order.assertBelongsToClient(99L));
        assertEquals("No puedes cancelar pedidos de otro cliente", ex.getMessage());
    }

    // ── assertIsPending ────────────────────────────────────────────────────────

    @Test
    void assertIsPendingShouldPassWhenPendiente() {
        assertDoesNotThrow(() -> orderWith(EstadoPedidoModel.PENDIENTE).assertIsPending());
    }

    @Test
    void assertIsPendingShouldFailWhenEnPreparacion() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderWith(EstadoPedidoModel.EN_PREPARACION).assertIsPending());
        assertEquals(DomainErrorMessage.ORDER_NOT_CANCELABLE.getMessage(), ex.getMessage());
    }

    // ── assertIsEnPreparacion ──────────────────────────────────────────────────

    @Test
    void assertIsEnPreparacionShouldPassWhenEnPreparacion() {
        assertDoesNotThrow(() -> orderWith(EstadoPedidoModel.EN_PREPARACION).assertIsEnPreparacion());
    }

    @Test
    void assertIsEnPreparacionShouldFailWhenPendiente() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderWith(EstadoPedidoModel.PENDIENTE).assertIsEnPreparacion());
        assertEquals(DomainErrorMessage.ORDER_NOT_IN_PREPARACION.getMessage(), ex.getMessage());
    }

    // ── assertIsListo ──────────────────────────────────────────────────────────

    @Test
    void assertIsListoShouldPassWhenListo() {
        assertDoesNotThrow(() -> orderWith(EstadoPedidoModel.LISTO).assertIsListo());
    }

    @Test
    void assertIsListoShouldFailWhenEnPreparacion() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderWith(EstadoPedidoModel.EN_PREPARACION).assertIsListo());
        assertEquals(DomainErrorMessage.ORDER_NOT_LISTO.getMessage(), ex.getMessage());
    }

    // ── assertAssignedEmployeeCanMarkReady ─────────────────────────────────────

    @Test
    void assertAssignedEmployeeCanMarkReadyShouldPassForAssignedEmployee() {
        OrderModel order = orderWithEmployee(EstadoPedidoModel.EN_PREPARACION, 100L);
        assertDoesNotThrow(() -> order.assertAssignedEmployeeCanMarkReady(100L));
    }

    @Test
    void assertAssignedEmployeeCanMarkReadyShouldFailForOtherEmployee() {
        OrderModel order = orderWithEmployee(EstadoPedidoModel.EN_PREPARACION, 100L);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> order.assertAssignedEmployeeCanMarkReady(999L));
        assertEquals("Solo el empleado asignado puede marcar LISTO", ex.getMessage());
    }

    // ── assertAssignedEmployeeCanDeliver ───────────────────────────────────────

    @Test
    void assertAssignedEmployeeCanDeliverShouldPassForAssignedEmployee() {
        OrderModel order = orderWithEmployee(EstadoPedidoModel.LISTO, 100L);
        assertDoesNotThrow(() -> order.assertAssignedEmployeeCanDeliver(100L));
    }

    @Test
    void assertAssignedEmployeeCanDeliverShouldFailForOtherEmployee() {
        OrderModel order = orderWithEmployee(EstadoPedidoModel.LISTO, 100L);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> order.assertAssignedEmployeeCanDeliver(999L));
        assertEquals("Solo el empleado asignado puede entregar", ex.getMessage());
    }

    // ── assertPin ──────────────────────────────────────────────────────────────

    @Test
    void assertPinShouldPassWhenPinMatches() {
        OrderModel order = OrderModel.builder()
                .id(1L).idRestaurante(1L).idCliente(10L)
                .estado(EstadoPedidoModel.LISTO).pinSeguridad("123456")
                .fechaCreacion(LocalDateTime.now()).items(List.of()).build();
        assertDoesNotThrow(() -> order.assertPin("123456"));
    }

    @Test
    void assertPinShouldFailWhenPinDoesNotMatch() {
        OrderModel order = OrderModel.builder()
                .id(1L).idRestaurante(1L).idCliente(10L)
                .estado(EstadoPedidoModel.LISTO).pinSeguridad("123456")
                .fechaCreacion(LocalDateTime.now()).items(List.of()).build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> order.assertPin("999999"));
        assertEquals("PIN invalido", ex.getMessage());
    }

    // ── OrderItemModel.assertNotEmpty ──────────────────────────────────────────

    @Test
    void assertNotEmptyShouldPassForNonEmptyList() {
        assertDoesNotThrow(() ->
                OrderItemModel.assertNotEmpty(List.of(OrderItemModel.builder().idPlato(1L).cantidad(1).build())));
    }

    @Test
    void assertNotEmptyShouldFailForEmptyList() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> OrderItemModel.assertNotEmpty(List.of()));
        assertEquals("El pedido debe contener platos", ex.getMessage());
    }

    @Test
    void assertNotEmptyShouldFailForNullList() {
        assertThrows(BusinessRuleException.class, () -> OrderItemModel.assertNotEmpty(null));
    }
}
