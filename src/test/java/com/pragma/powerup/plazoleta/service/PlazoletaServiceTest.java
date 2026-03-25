package com.pragma.powerup.plazoleta.service;

import com.pragma.powerup.plazoleta.client.AuthHeaderProvider;
import com.pragma.powerup.plazoleta.client.MensajeriaClient;
import com.pragma.powerup.plazoleta.client.TrazabilidadClient;
import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.domain.EstadoPedido;
import com.pragma.powerup.plazoleta.domain.OrderEntity;
import com.pragma.powerup.plazoleta.domain.RestaurantEntity;
import com.pragma.powerup.plazoleta.domain.EmployeeRestaurantEntity;
import com.pragma.powerup.plazoleta.repository.DishRepository;
import com.pragma.powerup.plazoleta.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.repository.OrderRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import com.pragma.powerup.plazoleta.security.AuthUtils;
import com.pragma.powerup.plazoleta.security.Rol;
import com.pragma.powerup.plazoleta.security.UsuarioPrincipal;
import com.pragma.powerup.plazoleta.service.pin.PinGenerator;
import com.pragma.powerup.plazoleta.web.dto.CreateOrderRequest;
import com.pragma.powerup.plazoleta.web.dto.DeliverOrderRequest;
import com.pragma.powerup.plazoleta.web.dto.AssignEmployeeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PlazoletaServiceTest {

    private RestaurantRepository restaurantRepository;
    private DishRepository dishRepository;
    private OrderRepository orderRepository;
    private EmployeeRestaurantRepository employeeRestaurantRepository;
    private AuthUtils authUtils;
    private UsuariosClient usuariosClient;
    private TrazabilidadClient trazabilidadClient;
    private MensajeriaClient mensajeriaClient;
    private PinGenerator pinGenerator;
    private AuthHeaderProvider authHeaderProvider;
    private PlazoletaService service;

    @BeforeEach
    void setUp() {
        restaurantRepository = mock(RestaurantRepository.class);
        dishRepository = mock(DishRepository.class);
        orderRepository = mock(OrderRepository.class);
        employeeRestaurantRepository = mock(EmployeeRestaurantRepository.class);
        authUtils = mock(AuthUtils.class);
        usuariosClient = mock(UsuariosClient.class);
        trazabilidadClient = mock(TrazabilidadClient.class);
        mensajeriaClient = mock(MensajeriaClient.class);
        pinGenerator = mock(PinGenerator.class);
        authHeaderProvider = mock(AuthHeaderProvider.class);
        service = new PlazoletaService(
                restaurantRepository,
                dishRepository,
                orderRepository,
                employeeRestaurantRepository,
                authUtils,
                usuariosClient,
                trazabilidadClient,
                mensajeriaClient,
                pinGenerator,
                authHeaderProvider);
    }

    @Test
    void createOrderShouldFailWhenClientHasActiveOrder() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(10L, "c@x.com", Rol.CLIENTE));
        when(orderRepository.existsByIdClienteAndEstadoIn(eq(10L), anyCollection())).thenReturn(true);

        CreateOrderRequest request = new CreateOrderRequest(1L, "3001234567", List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createOrder(request));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void cancelOrderShouldFailWhenOrderIsNotPending() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(10L, "c@x.com", Rol.CLIENTE));
        OrderEntity order = new OrderEntity();
        order.setId(99L);
        order.setIdCliente(10L);
        order.setEstado(EstadoPedido.EN_PREPARACION);
        order.setFechaCreacion(LocalDateTime.now());
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);
        order.setRestaurant(restaurant);
        when(orderRepository.findById(99L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.cancelOrder(99L));
        assertEquals(400, ex.getStatus().value());
        assertTrue(ex.getReason().contains("no puede cancelarse"));
    }

    @Test
    void cancelOrderShouldFailWhenClientIsNotOwner() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(99L, "other@x.com", Rol.CLIENTE));
        OrderEntity order = new OrderEntity();
        order.setId(77L);
        order.setIdCliente(10L);
        order.setEstado(EstadoPedido.PENDIENTE);
        order.setFechaCreacion(LocalDateTime.now());
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);
        order.setRestaurant(restaurant);
        when(orderRepository.findById(77L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.cancelOrder(77L));
        assertEquals(403, ex.getStatus().value());
        assertTrue(ex.getReason().contains("otro cliente"));
    }

    @Test
    void createOrderShouldFailWhenRoleIsNotCliente() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(10L, "emp@x.com", Rol.EMPLEADO));
        CreateOrderRequest request = new CreateOrderRequest(1L, "3001234567", List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createOrder(request));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void createRestaurantShouldFailWhenOwnerIsNotPropietario() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(1L, "admin@test.local", Rol.ADMINISTRADOR));
        when(usuariosClient.validarRolPropietario(200L)).thenReturn(false);

        var request = new com.pragma.powerup.plazoleta.web.dto.CreateRestaurantRequest(
                "Restaurante Uno", "123456", "Calle 1", "+573001112233", "http://logo", 200L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createRestaurant(request));
        assertEquals(400, ex.getStatus().value());
        assertTrue(ex.getReason().contains("rol PROPIETARIO"));
    }

    @Test
    void takeOrderConcurrency_secondEmployeeShouldFail() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        OrderEntity pending = new OrderEntity();
        pending.setId(10L);
        pending.setRestaurant(restaurant);
        pending.setIdCliente(50L);
        pending.setEstado(EstadoPedido.PENDIENTE);
        pending.setIdEmpleadoAsignado(null);
        pending.setFechaCreacion(LocalDateTime.now());

        OrderEntity accepted = new OrderEntity();
        accepted.setId(10L);
        accepted.setRestaurant(restaurant);
        accepted.setIdCliente(50L);
        accepted.setEstado(EstadoPedido.EN_PREPARACION);
        accepted.setIdEmpleadoAsignado(100L);
        accepted.setFechaCreacion(LocalDateTime.now());

        EmployeeRestaurantEntity employeeRestaurantA = new EmployeeRestaurantEntity();
        employeeRestaurantA.setIdEmpleado(100L);
        employeeRestaurantA.setRestaurant(restaurant);

        EmployeeRestaurantEntity employeeRestaurantB = new EmployeeRestaurantEntity();
        employeeRestaurantB.setIdEmpleado(200L);
        employeeRestaurantB.setRestaurant(restaurant);

        when(authUtils.currentUser()).thenReturn(
                new UsuarioPrincipal(100L, "empA@test.local", Rol.EMPLEADO),
                new UsuarioPrincipal(200L, "empB@test.local", Rol.EMPLEADO)
        );

        when(employeeRestaurantRepository.findFirstByIdEmpleado(100L)).thenReturn(Optional.of(employeeRestaurantA));
        when(employeeRestaurantRepository.findFirstByIdEmpleado(200L)).thenReturn(Optional.of(employeeRestaurantB));

        when(orderRepository.findById(10L)).thenReturn(
                Optional.of(pending),
                Optional.of(accepted),
                Optional.of(pending)
        );

        when(orderRepository.takeOrderIfPending(10L, EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, 100L)).thenReturn(1);
        when(orderRepository.takeOrderIfPending(10L, EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, 200L)).thenReturn(0);

        var resp1 = service.takeOrder(10L);
        assertEquals("EN_PREPARACION", resp1.estadoActual());
        assertEquals(100L, resp1.idEmpleadoAsignado());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.takeOrder(10L));
        assertEquals(400, ex.getStatus().value());
        assertTrue(ex.getReason().contains("ya fue asignado"));
    }

    @Test
    void markReadyShouldSendSmsAndSetPinAndState() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setRestaurant(restaurant);
        order.setIdCliente(50L);
        order.setEstado(EstadoPedido.EN_PREPARACION);
        order.setIdEmpleadoAsignado(100L);
        order.setTelefonoCliente("3001234567");
        order.setFechaCreacion(LocalDateTime.now());
        order.setPinSeguridad(null);

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(100L, "emp@test.local", Rol.EMPLEADO));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(pinGenerator.generarPin6Digitos()).thenReturn("123456");
        when(orderRepository.existsByPinSeguridad("123456")).thenReturn(false);

        var resp = service.markReady(10L);

        assertEquals("LISTO", resp.estadoActual());
        assertEquals(100L, resp.idEmpleadoAsignado());
        assertEquals("123456", order.getPinSeguridad());

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(mensajeriaClient).enviarSms(eq("3001234567"), msgCaptor.capture());
        assertTrue(msgCaptor.getValue().contains("123456"));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(trazabilidadClient).registrarEvento(payloadCaptor.capture());
        assertEquals("EN_PREPARACION", payloadCaptor.getValue().get("estadoAnterior"));
        assertEquals("LISTO", payloadCaptor.getValue().get("estadoNuevo"));
    }

    @Test
    void markReadyShouldRetryPinGenerationOnCollision() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setRestaurant(restaurant);
        order.setIdCliente(50L);
        order.setEstado(EstadoPedido.EN_PREPARACION);
        order.setIdEmpleadoAsignado(100L);
        order.setTelefonoCliente("3001234567");
        order.setFechaCreacion(LocalDateTime.now());
        order.setPinSeguridad(null);

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(100L, "emp@test.local", Rol.EMPLEADO));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        when(pinGenerator.generarPin6Digitos()).thenReturn("111111", "222222");
        when(orderRepository.existsByPinSeguridad("111111")).thenReturn(true);
        when(orderRepository.existsByPinSeguridad("222222")).thenReturn(false);

        var resp = service.markReady(10L);
        assertEquals("LISTO", resp.estadoActual());
        assertEquals("222222", order.getPinSeguridad());

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(mensajeriaClient).enviarSms(eq("3001234567"), msgCaptor.capture());
        assertTrue(msgCaptor.getValue().contains("222222"));
    }

    @Test
    void markReadyShouldNotPersistListoWhenSmsFails() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setRestaurant(restaurant);
        order.setIdCliente(50L);
        order.setEstado(EstadoPedido.EN_PREPARACION);
        order.setIdEmpleadoAsignado(100L);
        order.setTelefonoCliente("3001234567");
        order.setFechaCreacion(LocalDateTime.now());
        order.setPinSeguridad(null);

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(100L, "emp@test.local", Rol.EMPLEADO));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(pinGenerator.generarPin6Digitos()).thenReturn("123456");
        when(orderRepository.existsByPinSeguridad("123456")).thenReturn(false);
        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SMS failed"))
                .when(mensajeriaClient)
                .enviarSms(eq("3001234567"), anyString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.markReady(10L));
        assertEquals(502, ex.getStatus().value());

        // Pedido no debe cambiar de estado ni persistir PIN.
        assertEquals(EstadoPedido.EN_PREPARACION, order.getEstado());
        assertNull(order.getPinSeguridad());

        verify(trazabilidadClient, never()).registrarEvento(anyMap());
    }

    @Test
    void deliverOrderShouldConsumePinAndMarkDelivered() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setRestaurant(restaurant);
        order.setIdCliente(50L);
        order.setEstado(EstadoPedido.LISTO);
        order.setIdEmpleadoAsignado(100L);
        order.setPinSeguridad("123456");
        order.setFechaCreacion(LocalDateTime.now().minusMinutes(10));

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(100L, "emp@test.local", Rol.EMPLEADO));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        var resp = service.deliverOrder(10L, new DeliverOrderRequest("123456"));

        assertEquals("ENTREGADO", resp.estadoActual());
        assertNull(order.getPinSeguridad());
        assertNotNull(order.getFechaEntrega());

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(trazabilidadClient).registrarEvento(payloadCaptor.capture());
        assertEquals("LISTO", payloadCaptor.getValue().get("estadoAnterior"));
        assertEquals("ENTREGADO", payloadCaptor.getValue().get("estadoNuevo"));
    }

    @Test
    void deliverOrderShouldFailWhenPinInvalid() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setRestaurant(restaurant);
        order.setIdCliente(50L);
        order.setEstado(EstadoPedido.LISTO);
        order.setIdEmpleadoAsignado(100L);
        order.setPinSeguridad("123456");
        order.setFechaCreacion(LocalDateTime.now().minusMinutes(10));

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(100L, "emp@test.local", Rol.EMPLEADO));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.deliverOrder(10L, new DeliverOrderRequest("999999")));

        assertEquals(400, ex.getStatus().value());
        assertEquals("PIN invalido", ex.getReason());
        assertEquals(EstadoPedido.LISTO, order.getEstado());
        assertEquals("123456", order.getPinSeguridad());
        verify(trazabilidadClient, never()).registrarEvento(anyMap());
    }

    @Test
    void cancelOrderShouldCancelWhenPendingAndOwner() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        OrderEntity order = new OrderEntity();
        order.setId(10L);
        order.setRestaurant(restaurant);
        order.setIdCliente(50L);
        order.setEstado(EstadoPedido.PENDIENTE);
        order.setFechaCreacion(LocalDateTime.now());

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(50L, "client@test.local", Rol.CLIENTE));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        var resp = service.cancelOrder(10L);
        assertEquals("CANCELADO", resp.estadoActual());
        assertEquals(EstadoPedido.CANCELADO, order.getEstado());

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(trazabilidadClient).registrarEvento(payloadCaptor.capture());
        assertEquals("PENDIENTE", payloadCaptor.getValue().get("estadoAnterior"));
        assertEquals("CANCELADO", payloadCaptor.getValue().get("estadoNuevo"));
    }

    @Test
    void assignEmployeeToRestaurant_shouldCreateAssignment() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(10L);
        restaurant.setIdPropietario(1L);

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(1L, "owner@test.local", Rol.PROPIETARIO));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
        when(usuariosClient.validarRolEmpleado(500L)).thenReturn(true);
        when(employeeRestaurantRepository.existsByIdEmpleadoAndRestaurant_Id(500L, 10L)).thenReturn(false);

        EmployeeRestaurantEntity saved = new EmployeeRestaurantEntity();
        saved.setId(77L);
        saved.setIdEmpleado(500L);
        saved.setRestaurant(restaurant);
        when(employeeRestaurantRepository.save(any(EmployeeRestaurantEntity.class))).thenReturn(saved);

        Long id = service.assignEmployeeToRestaurant(10L, new AssignEmployeeRequest(500L));
        assertEquals(77L, id);

        verify(employeeRestaurantRepository).save(any(EmployeeRestaurantEntity.class));
    }

    @Test
    void assignEmployeeToRestaurant_shouldFailWhenOwnerDoesNotOwnRestaurant() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(10L);
        restaurant.setIdPropietario(2L);

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(1L, "owner@test.local", Rol.PROPIETARIO));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.assignEmployeeToRestaurant(10L, new AssignEmployeeRequest(500L)));

        assertEquals(403, ex.getStatus().value());
        verify(usuariosClient, never()).validarRolEmpleado(anyLong());
        verify(employeeRestaurantRepository, never()).existsByIdEmpleadoAndRestaurant_Id(anyLong(), anyLong());
    }
}
