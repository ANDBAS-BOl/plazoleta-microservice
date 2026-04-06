package com.pragma.powerup.plazoleta.architecture;

import com.pragma.powerup.plazoleta.application.dto.request.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.application.handler.impl.CatalogHandler;
import com.pragma.powerup.plazoleta.application.handler.impl.OrderHandler;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
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

    @InjectMocks
    private CatalogHandler catalogHandler;

    @InjectMocks
    private OrderHandler orderHandler;

    @Test
    void shouldDelegateRestaurantCreationToCatalogUseCase() {
        when(catalogUseCasePort.createRestaurant(any())).thenReturn(77L);

        Long createdId = catalogHandler.createRestaurant(new CreateRestaurantRequest(
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

        var response = orderHandler.markReady(200L, 20L);

        assertEquals(200L, response.idPedido());
        assertEquals("LISTO", response.estadoActual());
        verify(orderUseCasePort).markReady(200L, 20L);
    }

    @Test
    void shouldDelegateTraceQueryToUseCasePort() {
        Map<String, Object> trace = Map.of("pedidoId", 500L, "eventos", List.of());
        when(orderTraceQueryUseCasePort.getTraceByOrderId(500L)).thenReturn(trace);

        Object result = orderHandler.trace(500L);

        assertEquals(trace, result);
        verify(orderTraceQueryUseCasePort).getTraceByOrderId(500L);
    }
}
