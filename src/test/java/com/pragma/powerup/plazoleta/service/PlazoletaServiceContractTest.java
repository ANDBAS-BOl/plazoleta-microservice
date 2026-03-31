package com.pragma.powerup.plazoleta.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.pragma.powerup.plazoleta.client.AuthHeaderProvider;
import com.pragma.powerup.plazoleta.client.MensajeriaClient;
import com.pragma.powerup.plazoleta.client.TrazabilidadClient;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.EstadoPedido;
import com.pragma.powerup.plazoleta.domain.OrderEntity;
import com.pragma.powerup.plazoleta.domain.RestaurantEntity;
import com.pragma.powerup.plazoleta.domain.usecase.CatalogUseCase;
import com.pragma.powerup.plazoleta.domain.usecase.OrderUseCase;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.OrderMessagingAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.OrderTraceQueryAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.OrderTraceabilityAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.UsuariosValidationAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter.CatalogJpaAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter.OrderJpaAdapter;
import com.pragma.powerup.plazoleta.repository.DishRepository;
import com.pragma.powerup.plazoleta.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.repository.OrderRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import com.pragma.powerup.plazoleta.infrastructure.security.AuthUtils;
import com.pragma.powerup.plazoleta.infrastructure.security.Rol;
import com.pragma.powerup.plazoleta.infrastructure.security.UsuarioPrincipal;
import com.pragma.powerup.plazoleta.service.pin.PinGenerator;
import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.web.dto.DeliverOrderRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.web.dto.OrderResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PlazoletaServiceContractTest {

    private WireMockServer wireMockServer;
    private String baseUrl;

    private RestaurantRepository restaurantRepository;
    private DishRepository dishRepository;
    private OrderRepository orderRepository;
    private EmployeeRestaurantRepository employeeRestaurantRepository;
    private AuthUtils authUtils;
    private UsuariosClient usuariosClient;
    private CatalogUseCasePort catalogUseCasePort;

    private TrazabilidadClient trazabilidadClient;
    private MensajeriaClient mensajeriaClient;
    private PinGenerator pinGenerator;
    private AuthHeaderProvider authHeaderProvider;
    private OrderUseCasePort orderUseCasePort;
    private OrderTraceQueryUseCasePort orderTraceQueryUseCasePort;
    private OrderEfficiencyUseCasePort orderEfficiencyUseCasePort;

    private PlazoletaService service;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        baseUrl = "http://127.0.0.1:" + wireMockServer.port();
        configureFor("127.0.0.1", wireMockServer.port());

        wireMockServer.resetAll();

        restaurantRepository = mock(RestaurantRepository.class);
        dishRepository = mock(DishRepository.class);
        orderRepository = mock(OrderRepository.class);
        employeeRestaurantRepository = mock(EmployeeRestaurantRepository.class);
        authUtils = mock(AuthUtils.class);
        usuariosClient = mock(UsuariosClient.class);
        catalogUseCasePort = mock(CatalogUseCasePort.class);
        pinGenerator = mock(PinGenerator.class);

        authHeaderProvider = () -> Optional.of("Bearer test-token");

        trazabilidadClient = new TrazabilidadClient(baseUrl, authHeaderProvider);
        mensajeriaClient = new MensajeriaClient(baseUrl, authHeaderProvider);
        orderUseCasePort = new OrderUseCase(
                new OrderJpaAdapter(
                        restaurantRepository,
                        dishRepository,
                        orderRepository,
                        employeeRestaurantRepository),
                new OrderTraceabilityAdapter(trazabilidadClient),
                new OrderMessagingAdapter(mensajeriaClient),
                pinGenerator::generarPin6Digitos
        );
        orderTraceQueryUseCasePort = idOrder -> new OrderTraceQueryAdapter(trazabilidadClient).getTraceByOrderId(idOrder);
        orderEfficiencyUseCasePort = ownerId -> new com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel(java.util.List.of());

        service = new PlazoletaService(
                restaurantRepository,
                dishRepository,
                orderRepository,
                employeeRestaurantRepository,
                catalogUseCasePort,
                orderUseCasePort,
                orderTraceQueryUseCasePort,
                orderEfficiencyUseCasePort,
                authUtils,
                usuariosClient
        );
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void markReadyShouldCallSmsAndTraceContracts() {
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

        stubFor(post(urlEqualTo("/api/v1/mensajeria/sms"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .withRequestBody(equalToJson("""
                        {
                          "phoneNumber": "3001234567",
                          "message": "Tu pedido esta listo. PIN: 123456"
                        }
                        """))
                .willReturn(aResponse().withStatus(200)));

        stubFor(post(urlEqualTo("/api/v1/trazabilidad/eventos"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .withRequestBody(equalToJson("""
                        {
                          "idPedido": 10,
                          "idCliente": 50,
                          "idRestaurante": 1,
                          "idEmpleado": 100,
                          "estadoAnterior": "EN_PREPARACION",
                          "estadoNuevo": "LISTO"
                        }
                        """))
                .willReturn(aResponse().withStatus(201)));

        OrderResponse resp = service.markReady(10L);

        assertEquals("LISTO", resp.estadoActual());
        assertEquals("123456", order.getPinSeguridad());

        com.github.tomakehurst.wiremock.client.WireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/mensajeria/sms")));
        com.github.tomakehurst.wiremock.client.WireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/trazabilidad/eventos")));
    }

    @Test
    void markReadyShouldNotCallTraceWhenSmsFails() {
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

        stubFor(post(urlEqualTo("/api/v1/mensajeria/sms"))
                .willReturn(aResponse().withStatus(502).withBody("Upstream error")));

        // Si Mensajería falla, no debe enviarse trazabilidad de transición a LISTO.
        assertThrows(Exception.class, () -> service.markReady(10L));

        assertEquals(EstadoPedido.EN_PREPARACION, order.getEstado());
        assertNull(order.getPinSeguridad());
        com.github.tomakehurst.wiremock.client.WireMock.verify(0, postRequestedFor(urlEqualTo("/api/v1/trazabilidad/eventos")));
    }

    @Test
    void deliverOrderShouldConsumePinAndCallTraceContract() {
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

        stubFor(post(urlEqualTo("/api/v1/trazabilidad/eventos"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .withRequestBody(equalToJson("""
                        {
                          "idPedido": 10,
                          "idCliente": 50,
                          "idRestaurante": 1,
                          "idEmpleado": 100,
                          "estadoAnterior": "LISTO",
                          "estadoNuevo": "ENTREGADO"
                        }
                        """))
                .willReturn(aResponse().withStatus(201)));

        OrderResponse resp = service.deliverOrder(10L, new DeliverOrderRequest("123456"));

        assertEquals("ENTREGADO", resp.estadoActual());
        assertNull(order.getPinSeguridad());
        assertNotNull(order.getFechaEntrega());

        com.github.tomakehurst.wiremock.client.WireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/trazabilidad/eventos")));
    }

    @Test
    void createRestaurantShouldValidateOwnerViaUsuariosContract() {
        // Usamos el UsuariosClient real para verificar el contrato HTTP.
        UsuariosClient realUsuariosClient = new UsuariosClient(baseUrl, authHeaderProvider);

        when(restaurantRepository.save(any(RestaurantEntity.class))).thenAnswer(invocation -> {
            RestaurantEntity entity = invocation.getArgument(0);
            entity.setId(999L);
            return entity;
        });

        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(1L, "admin@test.local", Rol.ADMINISTRADOR));

        stubFor(get(urlEqualTo("/api/v1/usuarios/200/validacion-propietario"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "idUsuario": 200,
                                  "rol": "PROPIETARIO",
                                  "propietarioValido": true
                                }
                                """)));

        PlazoletaService localService = new PlazoletaService(
                restaurantRepository,
                dishRepository,
                orderRepository,
                employeeRestaurantRepository,
                new CatalogUseCase(
                        new CatalogJpaAdapter(restaurantRepository, dishRepository),
                        new UsuariosValidationAdapter(realUsuariosClient)
                ),
                orderUseCasePort,
                orderTraceQueryUseCasePort,
                orderEfficiencyUseCasePort,
                authUtils,
                realUsuariosClient
        );

        Long id = localService.createRestaurant(new CreateRestaurantRequest(
                "Restaurante Uno",
                "123456",
                "Calle 1",
                "+573001112233",
                "http://logo",
                200L
        ));

        assertEquals(999L, id);
        com.github.tomakehurst.wiremock.client.WireMock.verify(1,
                getRequestedFor(urlEqualTo("/api/v1/usuarios/200/validacion-propietario")));
    }

    @Test
    void assignEmployeeShouldValidateEmployeeViaUsuariosContract() {
        UsuariosClient realUsuariosClient = new UsuariosClient(baseUrl, authHeaderProvider);

        stubFor(get(urlEqualTo("/api/v1/usuarios/500/validacion-empleado"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "idUsuario": 500,
                                  "rol": "EMPLEADO",
                                  "empleadoValido": true
                                }
                                """)));

        assertTrue(realUsuariosClient.validarRolEmpleado(500L));
        com.github.tomakehurst.wiremock.client.WireMock.verify(1,
                getRequestedFor(urlEqualTo("/api/v1/usuarios/500/validacion-empleado")));
    }

    @Test
    void getTraceShouldCallTrazabilidadReadContract() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(50L, "client@test.local", Rol.CLIENTE));

        stubFor(get(urlEqualTo("/api/v1/trazabilidad/pedidos/10"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "idPedido": 10,
                                  "eventos": []
                                }
                                """)));

        Object response = service.getTrace(10L);
        assertNotNull(response);

        com.github.tomakehurst.wiremock.client.WireMock.verify(1,
                getRequestedFor(urlEqualTo("/api/v1/trazabilidad/pedidos/10")));
    }
}

