package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.exception.AccessDeniedException;
import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.exception.InternalProcessException;
import com.pragma.powerup.plazoleta.domain.exception.ResourceNotFoundException;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.OrderMessagingPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.OrderPinGeneratorPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceabilityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderUseCaseTest {

    private OrderPersistencePort persistencePort;
    private OrderTraceabilityPort traceabilityPort;
    private OrderMessagingPort messagingPort;
    private OrderPinGeneratorPort pinGeneratorPort;
    private OrderUseCase orderUseCase;

    @BeforeEach
    void setUp() {
        persistencePort = mock(OrderPersistencePort.class);
        traceabilityPort = mock(OrderTraceabilityPort.class);
        messagingPort = mock(OrderMessagingPort.class);
        pinGeneratorPort = mock(OrderPinGeneratorPort.class);
        orderUseCase = new OrderUseCase(persistencePort, traceabilityPort, messagingPort, pinGeneratorPort);
    }

    // ========================
    // createOrder
    // ========================

    @Test
    void createOrderShouldFailWhenClientHasActiveOrder() {
        when(persistencePort.hasActiveOrder(10L)).thenReturn(true);

        List<OrderItemModel> items = List.of(OrderItemModel.builder().idPlato(1L).cantidad(1).build());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.createOrder(10L, 1L, "3001234567", items));
        assertTrue(ex.getMessage().contains("pedido en proceso"));
    }

    @Test
    void createOrderShouldFailWhenItemsAreEmpty() {
        when(persistencePort.hasActiveOrder(10L)).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.createOrder(10L, 1L, "3001234567", List.of()));
        assertTrue(ex.getMessage().contains("contener platos"));
    }

    @Test
    void createOrderShouldFailWhenItemsAreNull() {
        when(persistencePort.hasActiveOrder(10L)).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.createOrder(10L, 1L, "3001234567", null));
        assertTrue(ex.getMessage().contains("contener platos"));
    }

    @Test
    void createOrderShouldFailWhenRestaurantNotFound() {
        when(persistencePort.hasActiveOrder(10L)).thenReturn(false);
        when(persistencePort.findRestaurantById(99L)).thenReturn(Optional.empty());

        List<OrderItemModel> items = List.of(OrderItemModel.builder().idPlato(1L).cantidad(1).build());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderUseCase.createOrder(10L, 99L, "3001234567", items));
        assertTrue(ex.getMessage().contains("Restaurante"));
    }

    @Test
    void createOrderShouldFailWhenDishIsInactive() {
        when(persistencePort.hasActiveOrder(10L)).thenReturn(false);
        when(persistencePort.findRestaurantById(1L)).thenReturn(Optional.of(
                RestaurantModel.builder().id(1L).build()));
        when(persistencePort.findDishById(5L)).thenReturn(Optional.of(
                DishModel.builder().id(5L).idRestaurante(1L).activo(false).build()));

        List<OrderItemModel> items = List.of(OrderItemModel.builder().idPlato(5L).cantidad(1).build());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.createOrder(10L, 1L, "3001234567", items));
        assertTrue(ex.getMessage().contains("inactivos"));
    }

    @Test
    void createOrderShouldFailWhenDishFromDifferentRestaurant() {
        when(persistencePort.hasActiveOrder(10L)).thenReturn(false);
        when(persistencePort.findRestaurantById(1L)).thenReturn(Optional.of(
                RestaurantModel.builder().id(1L).build()));
        when(persistencePort.findDishById(5L)).thenReturn(Optional.of(
                DishModel.builder().id(5L).idRestaurante(2L).activo(true).build()));

        List<OrderItemModel> items = List.of(OrderItemModel.builder().idPlato(5L).cantidad(1).build());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.createOrder(10L, 1L, "3001234567", items));
        assertTrue(ex.getMessage().contains("un solo restaurante"));
    }

    @Test
    void createOrderShouldSucceedAndRegisterTrace() {
        when(persistencePort.hasActiveOrder(10L)).thenReturn(false);
        when(persistencePort.findRestaurantById(1L)).thenReturn(Optional.of(
                RestaurantModel.builder().id(1L).build()));
        when(persistencePort.findDishById(5L)).thenReturn(Optional.of(
                DishModel.builder().id(5L).idRestaurante(1L).activo(true).build()));

        OrderModel saved = OrderModel.builder()
                .id(100L).idRestaurante(1L).idCliente(10L)
                .estado(EstadoPedidoModel.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .items(List.of(OrderItemModel.builder().idPlato(5L).cantidad(2).build()))
                .build();
        when(persistencePort.saveOrder(any())).thenReturn(saved);

        List<OrderItemModel> items = List.of(OrderItemModel.builder().idPlato(5L).cantidad(2).build());
        OrderModel result = orderUseCase.createOrder(10L, 1L, "3001234567", items);

        assertEquals(EstadoPedidoModel.PENDIENTE, result.getEstado());
        assertEquals(100L, result.getId());
        verify(traceabilityPort).registerTransition(eq(saved), isNull(), eq(EstadoPedidoModel.PENDIENTE));
    }

    // ========================
    // cancelOrder
    // ========================

    @Test
    void cancelOrderShouldFailWhenOrderNotFound() {
        when(persistencePort.findOrderById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderUseCase.cancelOrder(99L, 10L));
        assertTrue(ex.getMessage().contains("Pedido"));
    }

    @Test
    void cancelOrderShouldFailWhenClientIsNotOwner() {
        OrderModel order = OrderModel.builder()
                .id(10L).idCliente(50L).estado(EstadoPedidoModel.PENDIENTE)
                .idRestaurante(1L).fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderUseCase.cancelOrder(10L, 99L));
        assertTrue(ex.getMessage().contains("otro cliente"));
    }

    @Test
    void cancelOrderShouldFailWhenOrderIsNotPending() {
        OrderModel order = OrderModel.builder()
                .id(10L).idCliente(50L).estado(EstadoPedidoModel.EN_PREPARACION)
                .idRestaurante(1L).fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.cancelOrder(10L, 50L));
        assertTrue(ex.getMessage().contains("no puede cancelarse"));
    }

    @Test
    void cancelOrderShouldSucceedWhenPendingAndOwner() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.PENDIENTE)
                .fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        OrderModel saved = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.CANCELADO)
                .fechaCreacion(order.getFechaCreacion()).items(List.of())
                .build();
        when(persistencePort.saveOrder(any())).thenReturn(saved);

        OrderModel result = orderUseCase.cancelOrder(10L, 50L);

        assertEquals(EstadoPedidoModel.CANCELADO, result.getEstado());
        verify(persistencePort).saveOrder(argThat(o -> o.getEstado() == EstadoPedidoModel.CANCELADO));
        verify(traceabilityPort).registerTransition(eq(saved), eq(EstadoPedidoModel.PENDIENTE), eq(EstadoPedidoModel.CANCELADO));
    }

    // ========================
    // takeOrder (concurrency)
    // ========================

    @Test
    void takeOrderShouldSucceedForFirstEmployee() {
        OrderModel pending = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.PENDIENTE).items(List.of())
                .fechaCreacion(LocalDateTime.now())
                .build();
        OrderModel accepted = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.EN_PREPARACION).idEmpleadoAsignado(100L)
                .items(List.of()).fechaCreacion(pending.getFechaCreacion())
                .build();

        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(pending), Optional.of(accepted));
        when(persistencePort.findRestaurantIdByEmployee(100L)).thenReturn(Optional.of(1L));
        when(persistencePort.takeOrderIfPending(10L, 100L)).thenReturn(1);

        OrderModel result = orderUseCase.takeOrder(10L, 100L);

        assertEquals(EstadoPedidoModel.EN_PREPARACION, result.getEstado());
        assertEquals(100L, result.getIdEmpleadoAsignado());
        verify(traceabilityPort).registerTransition(eq(accepted), eq(EstadoPedidoModel.PENDIENTE), eq(EstadoPedidoModel.EN_PREPARACION));
    }

    @Test
    void takeOrderShouldFailForSecondEmployee() {
        OrderModel pending = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.PENDIENTE).items(List.of())
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(pending));
        when(persistencePort.findRestaurantIdByEmployee(200L)).thenReturn(Optional.of(1L));
        when(persistencePort.takeOrderIfPending(10L, 200L)).thenReturn(0);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.takeOrder(10L, 200L));
        assertTrue(ex.getMessage().contains("ya fue asignado"));
    }

    @Test
    void takeOrderShouldFailWhenEmployeeFromDifferentRestaurant() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.PENDIENTE).items(List.of())
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));
        when(persistencePort.findRestaurantIdByEmployee(100L)).thenReturn(Optional.of(2L));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderUseCase.takeOrder(10L, 100L));
        assertTrue(ex.getMessage().contains("tu restaurante"));
    }

    @Test
    void takeOrderShouldFailWhenEmployeeNotAssignedToAnyRestaurant() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.PENDIENTE).items(List.of())
                .fechaCreacion(LocalDateTime.now())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));
        when(persistencePort.findRestaurantIdByEmployee(100L)).thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderUseCase.takeOrder(10L, 100L));
        assertTrue(ex.getMessage().contains("no esta asignado"));
    }

    // ========================
    // markReady (SMS + PIN)
    // ========================

    @Test
    void markReadyShouldSendSmsAndSetPinAndTransitionToListo() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.EN_PREPARACION).idEmpleadoAsignado(100L)
                .telefonoCliente("3001234567").fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));
        when(pinGeneratorPort.generatePin6Digits()).thenReturn("123456");
        when(persistencePort.existsPinSeguridad("123456")).thenReturn(false);

        OrderModel saved = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.LISTO).idEmpleadoAsignado(100L)
                .telefonoCliente("3001234567").pinSeguridad("123456")
                .fechaCreacion(order.getFechaCreacion()).items(List.of())
                .build();
        when(persistencePort.saveOrder(any())).thenReturn(saved);

        OrderModel result = orderUseCase.markReady(10L, 100L);

        assertEquals(EstadoPedidoModel.LISTO, result.getEstado());
        assertEquals("123456", result.getPinSeguridad());
        verify(messagingPort).sendOrderReadyPin("3001234567", "123456");
        verify(traceabilityPort).registerTransition(eq(saved), eq(EstadoPedidoModel.EN_PREPARACION), eq(EstadoPedidoModel.LISTO));
    }

    @Test
    void markReadyShouldRetryPinGenerationOnCollision() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.EN_PREPARACION).idEmpleadoAsignado(100L)
                .telefonoCliente("3001234567").fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));
        when(pinGeneratorPort.generatePin6Digits()).thenReturn("111111", "222222");
        when(persistencePort.existsPinSeguridad("111111")).thenReturn(true);
        when(persistencePort.existsPinSeguridad("222222")).thenReturn(false);

        OrderModel saved = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.LISTO).idEmpleadoAsignado(100L)
                .telefonoCliente("3001234567").pinSeguridad("222222")
                .fechaCreacion(order.getFechaCreacion()).items(List.of())
                .build();
        when(persistencePort.saveOrder(any())).thenReturn(saved);

        OrderModel result = orderUseCase.markReady(10L, 100L);

        assertEquals("222222", result.getPinSeguridad());
        verify(messagingPort).sendOrderReadyPin("3001234567", "222222");
    }

    @Test
    void markReadyShouldNotPersistListoWhenSmsFails() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.EN_PREPARACION).idEmpleadoAsignado(100L)
                .telefonoCliente("3001234567").fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));
        when(pinGeneratorPort.generatePin6Digits()).thenReturn("123456");
        when(persistencePort.existsPinSeguridad("123456")).thenReturn(false);
        doThrow(new InternalProcessException("SMS failed"))
                .when(messagingPort).sendOrderReadyPin(anyString(), anyString());

        assertThrows(InternalProcessException.class, () -> orderUseCase.markReady(10L, 100L));

        verify(persistencePort, never()).saveOrder(any());
        verify(traceabilityPort, never()).registerTransition(any(), any(), any());
    }

    @Test
    void markReadyShouldFailWhenEmployeeIsNotAssigned() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.EN_PREPARACION).idEmpleadoAsignado(100L)
                .telefonoCliente("3001234567").fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderUseCase.markReady(10L, 999L));
        assertTrue(ex.getMessage().contains("empleado asignado"));
    }

    @Test
    void markReadyShouldFailWhenOrderIsNotEnPreparacion() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.PENDIENTE).idEmpleadoAsignado(100L)
                .telefonoCliente("3001234567").fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.markReady(10L, 100L));
        assertTrue(ex.getMessage().contains("EN_PREPARACION"));
    }

    @Test
    void markReadyShouldFailWhenAllPinAttemptsExhausted() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.EN_PREPARACION).idEmpleadoAsignado(100L)
                .telefonoCliente("3001234567").fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));
        when(pinGeneratorPort.generatePin6Digits()).thenReturn("999999");
        when(persistencePort.existsPinSeguridad("999999")).thenReturn(true);

        InternalProcessException ex = assertThrows(InternalProcessException.class,
                () -> orderUseCase.markReady(10L, 100L));
        assertTrue(ex.getMessage().contains("PIN unico"));
    }

    // ========================
    // deliverOrder (PIN)
    // ========================

    @Test
    void deliverOrderShouldConsumePinAndMarkDelivered() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.LISTO).idEmpleadoAsignado(100L)
                .pinSeguridad("123456").fechaCreacion(LocalDateTime.now().minusMinutes(10)).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        OrderModel saved = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.ENTREGADO).idEmpleadoAsignado(100L)
                .pinSeguridad(null).fechaCreacion(order.getFechaCreacion())
                .fechaEntrega(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.saveOrder(any())).thenReturn(saved);

        OrderModel result = orderUseCase.deliverOrder(10L, 100L, "123456");

        assertEquals(EstadoPedidoModel.ENTREGADO, result.getEstado());
        assertNull(result.getPinSeguridad());
        assertNotNull(result.getFechaEntrega());
        verify(persistencePort).saveOrder(argThat(o ->
                o.getEstado() == EstadoPedidoModel.ENTREGADO && o.getPinSeguridad() == null));
        verify(traceabilityPort).registerTransition(eq(saved), eq(EstadoPedidoModel.LISTO), eq(EstadoPedidoModel.ENTREGADO));
    }

    @Test
    void deliverOrderShouldFailWhenPinIsInvalid() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.LISTO).idEmpleadoAsignado(100L)
                .pinSeguridad("123456").fechaCreacion(LocalDateTime.now().minusMinutes(10)).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.deliverOrder(10L, 100L, "999999"));
        assertEquals("PIN invalido", ex.getMessage());
        verify(persistencePort, never()).saveOrder(any());
        verify(traceabilityPort, never()).registerTransition(any(), any(), any());
    }

    @Test
    void deliverOrderShouldFailWhenOrderIsNotListo() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.EN_PREPARACION).idEmpleadoAsignado(100L)
                .fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderUseCase.deliverOrder(10L, 100L, "123456"));
        assertTrue(ex.getMessage().contains("LISTO"));
    }

    @Test
    void deliverOrderShouldFailWhenEmployeeIsNotAssigned() {
        OrderModel order = OrderModel.builder()
                .id(10L).idRestaurante(1L).idCliente(50L)
                .estado(EstadoPedidoModel.LISTO).idEmpleadoAsignado(100L)
                .pinSeguridad("123456").fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();
        when(persistencePort.findOrderById(10L)).thenReturn(Optional.of(order));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderUseCase.deliverOrder(10L, 999L, "123456"));
        assertTrue(ex.getMessage().contains("empleado asignado"));
    }

    // ========================
    // listOrdersByStatus
    // ========================

    @Test
    void listOrdersByStatusShouldDelegateToPort() {
        when(persistencePort.findRestaurantIdByEmployee(100L)).thenReturn(Optional.of(1L));
        PageResult<OrderModel> expected = new PageResult<>(List.of(), 0, 10, 0, 0);
        when(persistencePort.listOrdersByStatus(eq(1L), eq(EstadoPedidoModel.PENDIENTE), any()))
                .thenReturn(expected);

        PageResult<OrderModel> result = orderUseCase.listOrdersByStatus(
                100L, EstadoPedidoModel.PENDIENTE, new PaginationParams(0, 10));

        assertEquals(0, result.totalElements());
        verify(persistencePort).listOrdersByStatus(1L, EstadoPedidoModel.PENDIENTE, new PaginationParams(0, 10));
    }

    @Test
    void listOrdersByStatusShouldFailWhenEmployeeNotAssigned() {
        when(persistencePort.findRestaurantIdByEmployee(100L)).thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> orderUseCase.listOrdersByStatus(100L, EstadoPedidoModel.PENDIENTE, new PaginationParams(0, 10)));
        assertTrue(ex.getMessage().contains("no esta asignado"));
    }
}
