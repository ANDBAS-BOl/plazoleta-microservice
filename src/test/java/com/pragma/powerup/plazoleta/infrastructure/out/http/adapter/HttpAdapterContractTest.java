package com.pragma.powerup.plazoleta.infrastructure.out.http.adapter;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.pragma.powerup.plazoleta.domain.exception.InternalProcessException;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.AuthHeaderProvider;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.MensajeriaClient;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.TrazabilidadClient;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.UsuariosClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpAdapterContractTest {

    private WireMockServer wireMockServer;
    private String baseUrl;
    private AuthHeaderProvider authHeaderProvider;

    private OrderMessagingAdapter messagingAdapter;
    private OrderTraceabilityAdapter traceabilityAdapter;
    private OrderTraceQueryAdapter traceQueryAdapter;
    private UsuariosValidationAdapter usuariosAdapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        baseUrl = "http://127.0.0.1:" + wireMockServer.port();
        configureFor("127.0.0.1", wireMockServer.port());

        authHeaderProvider = () -> Optional.of("Bearer test-token");

        MensajeriaClient mensajeriaClient = new MensajeriaClient(baseUrl, authHeaderProvider);
        TrazabilidadClient trazabilidadClient = new TrazabilidadClient(baseUrl, authHeaderProvider);
        UsuariosClient usuariosClient = new UsuariosClient(baseUrl, authHeaderProvider);

        messagingAdapter = new OrderMessagingAdapter(mensajeriaClient);
        traceabilityAdapter = new OrderTraceabilityAdapter(trazabilidadClient);
        traceQueryAdapter = new OrderTraceQueryAdapter(trazabilidadClient);
        usuariosAdapter = new UsuariosValidationAdapter(usuariosClient);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    // ========================
    // Mensajeria SMS contract
    // ========================

    @Test
    void sendOrderReadyPinShouldCallSmsEndpointWithCorrectPayload() {
        stubFor(post(urlEqualTo("/api/v1/mensajeria/sms"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .withRequestBody(equalToJson("""
                        {
                          "phoneNumber": "3001234567",
                          "message": "Tu pedido esta listo. PIN: 123456"
                        }
                        """))
                .willReturn(aResponse().withStatus(200)));

        messagingAdapter.sendOrderReadyPin("3001234567", "123456");

        verify(1, postRequestedFor(urlEqualTo("/api/v1/mensajeria/sms")));
    }

    @Test
    void sendOrderReadyPinShouldThrowWhenSmsFails() {
        stubFor(post(urlEqualTo("/api/v1/mensajeria/sms"))
                .willReturn(aResponse().withStatus(502).withBody("Upstream error")));

        assertThrows(InternalProcessException.class,
                () -> messagingAdapter.sendOrderReadyPin("3001234567", "123456"));
    }

    // ========================
    // Trazabilidad write contract
    // ========================

    @Test
    void registerTransitionShouldCallTraceEndpointWithCorrectPayload() {
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

        OrderModel order = OrderModel.builder()
                .id(10L).idCliente(50L).idRestaurante(1L).idEmpleadoAsignado(100L)
                .estado(EstadoPedidoModel.LISTO).fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();

        traceabilityAdapter.registerTransition(order, EstadoPedidoModel.EN_PREPARACION, EstadoPedidoModel.LISTO);

        verify(1, postRequestedFor(urlEqualTo("/api/v1/trazabilidad/eventos")));
    }

    @Test
    void registerTransitionShouldNotThrowWhenTraceServiceFails() {
        stubFor(post(urlEqualTo("/api/v1/trazabilidad/eventos"))
                .willReturn(aResponse().withStatus(500)));

        OrderModel order = OrderModel.builder()
                .id(10L).idCliente(50L).idRestaurante(1L).idEmpleadoAsignado(100L)
                .estado(EstadoPedidoModel.LISTO).fechaCreacion(LocalDateTime.now()).items(List.of())
                .build();

        assertDoesNotThrow(() -> traceabilityAdapter.registerTransition(
                order, EstadoPedidoModel.EN_PREPARACION, EstadoPedidoModel.LISTO));
    }

    // ========================
    // Trazabilidad read contract
    // ========================

    @Test
    void getTraceByOrderIdShouldCallTrazabilidadReadEndpoint() {
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

        Object result = traceQueryAdapter.getTraceByOrderId(10L);
        assertNotNull(result);

        verify(1, getRequestedFor(urlEqualTo("/api/v1/trazabilidad/pedidos/10")));
    }

    // ========================
    // Usuarios validation contracts
    // ========================

    @Test
    void isPropietarioShouldCallUsuariosValidationEndpoint() {
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

        assertTrue(usuariosAdapter.isPropietario(200L));

        verify(1, getRequestedFor(urlEqualTo("/api/v1/usuarios/200/validacion-propietario")));
    }

    @Test
    void isEmpleadoShouldCallUsuariosValidationEndpoint() {
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

        assertTrue(usuariosAdapter.isEmpleado(500L));

        verify(1, getRequestedFor(urlEqualTo("/api/v1/usuarios/500/validacion-empleado")));
    }

    @Test
    void isPropietarioShouldReturnFalseWhenNotPropietario() {
        stubFor(get(urlEqualTo("/api/v1/usuarios/300/validacion-propietario"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "idUsuario": 300,
                                  "rol": "CLIENTE",
                                  "propietarioValido": false
                                }
                                """)));

        assertFalse(usuariosAdapter.isPropietario(300L));
    }
}
