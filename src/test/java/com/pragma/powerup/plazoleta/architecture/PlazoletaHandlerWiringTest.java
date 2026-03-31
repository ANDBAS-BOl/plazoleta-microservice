package com.pragma.powerup.plazoleta.architecture;

import com.pragma.powerup.plazoleta.application.handler.impl.PlazoletaHandler;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.infrastructure.security.AuthUtils;
import com.pragma.powerup.plazoleta.infrastructure.security.Rol;
import com.pragma.powerup.plazoleta.infrastructure.security.UsuarioPrincipal;
import com.pragma.powerup.plazoleta.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.web.dto.CreateRestaurantRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlazoletaHandlerWiringTest {

    @Mock
    private CatalogUseCasePort catalogUseCasePort;
    @Mock
    private OrderUseCasePort orderUseCasePort;
    @Mock
    private OrderTraceQueryUseCasePort orderTraceQueryUseCasePort;
    @Mock
    private OrderEfficiencyUseCasePort orderEfficiencyUseCasePort;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private EmployeeRestaurantRepository employeeRestaurantRepository;
    @Mock
    private UsuariosClient usuariosClient;
    @Mock
    private AuthUtils authUtils;

    @InjectMocks
    private PlazoletaHandler handler;

    @Test
    void shouldDelegateRestaurantCreationToCatalogUseCase() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(10L, "admin@test.com", Rol.ADMINISTRADOR));
        when(catalogUseCasePort.createRestaurant(any())).thenReturn(77L);

        Long createdId = handler.createRestaurant(new CreateRestaurantRequest(
                "Mi Restaurante",
                "123456",
                "Calle 1",
                "+573001234567",
                "https://logo.test",
                99L));

        assertEquals(77L, createdId);
        verify(catalogUseCasePort).createRestaurant(any());
    }

    @Test
    void shouldDelegateMarkReadyToOrderUseCaseAndMapResponse() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(20L, "empleado@test.com", Rol.EMPLEADO));
        OrderModel model = OrderModel.builder()
                .id(200L)
                .idRestaurante(8L)
                .idCliente(30L)
                .estado(EstadoPedidoModel.LISTO)
                .fechaCreacion(LocalDateTime.now().minusMinutes(10))
                .idEmpleadoAsignado(20L)
                .items(List.of(OrderItemModel.builder()
                        .idPlato(2L)
                        .nombrePlato("Pizza")
                        .cantidad(1)
                        .build()))
                .build();
        when(orderUseCasePort.markReady(200L, 20L)).thenReturn(model);

        var response = handler.markReady(200L);

        assertEquals(200L, response.idPedido());
        assertEquals("LISTO", response.estadoActual());
        verify(orderUseCasePort).markReady(200L, 20L);
    }

    @Test
    void shouldDelegateTraceQueryToUseCasePort() {
        when(authUtils.currentUser()).thenReturn(new UsuarioPrincipal(30L, "cliente@test.com", Rol.CLIENTE));
        Map<String, Object> trace = Map.of("pedidoId", 500L, "eventos", List.of());
        when(orderTraceQueryUseCasePort.getTraceByOrderId(500L)).thenReturn(trace);

        Object result = handler.trace(500L);

        assertEquals(trace, result);
        verify(orderTraceQueryUseCasePort).getTraceByOrderId(500L);
    }
}
