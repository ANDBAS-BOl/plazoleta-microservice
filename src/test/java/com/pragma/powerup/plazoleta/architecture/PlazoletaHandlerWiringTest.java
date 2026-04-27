package com.pragma.powerup.plazoleta.architecture;

import com.pragma.powerup.plazoleta.application.dto.request.CreateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.application.dto.request.UpdateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.response.OrderResponse;
import com.pragma.powerup.plazoleta.application.mapper.IPlazoletaDtoMapper;
import com.pragma.powerup.plazoleta.application.handler.impl.CatalogHandler;
import com.pragma.powerup.plazoleta.application.handler.impl.OrderHandler;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlazoletaHandlerWiringTest {

    @Mock
    private CatalogUseCasePort catalogUseCasePort;
    @Mock
    private IPlazoletaDtoMapper plazoletaDtoMapper;
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
        when(plazoletaDtoMapper.toRestaurantModel(any(CreateRestaurantRequest.class))).thenReturn(
                RestaurantModel.builder()
                        .nombre("Mi Restaurante")
                        .nit("123456")
                        .direccion("Calle 1")
                        .telefono("+573001234567")
                        .urlLogo("https://logo.test")
                        .idPropietario(99L)
                        .build());
        when(catalogUseCasePort.createRestaurant(any())).thenReturn(77L);

        Long createdId = catalogHandler.createRestaurant(new CreateRestaurantRequest(
                "Mi Restaurante",
                "123456",
                "Calle 1",
                "+573001234567",
                "https://logo.test",
                99L));

        assertEquals(77L, createdId);
        verify(plazoletaDtoMapper).toRestaurantModel(any(CreateRestaurantRequest.class));
        verify(catalogUseCasePort).createRestaurant(any());
    }

    @Test
    void shouldDelegateDishCreationToCatalogUseCase() {
        when(plazoletaDtoMapper.toDishModel(any(CreateDishRequest.class))).thenReturn(
                DishModel.builder()
                        .nombre("Pizza")
                        .precio(BigDecimal.TEN)
                        .descripcion("Queso")
                        .urlImagen("https://img.test/pizza")
                        .categoria("Principal")
                        .idRestaurante(3L)
                        .build());
        when(catalogUseCasePort.createDish(any(DishModel.class), eq(5L))).thenReturn(42L);

        Long createdId = catalogHandler.createDish(
                new CreateDishRequest(
                        "Pizza",
                        BigDecimal.TEN,
                        "Queso",
                        "https://img.test/pizza",
                        "Principal",
                        3L),
                5L);

        assertEquals(42L, createdId);
        verify(plazoletaDtoMapper).toDishModel(any(CreateDishRequest.class));
        verify(catalogUseCasePort).createDish(any(DishModel.class), eq(5L));
    }

    @Test
    void shouldDelegateDishUpdateToCatalogUseCase() {
        UpdateDishRequest request = new UpdateDishRequest(BigDecimal.valueOf(15.5), "Descripcion actualizada");
        DishModel patch = DishModel.builder()
                .precio(BigDecimal.valueOf(15.5))
                .descripcion("Descripcion actualizada")
                .build();
        when(plazoletaDtoMapper.toDishUpdateModel(any(UpdateDishRequest.class))).thenReturn(patch);

        catalogHandler.updateDish(10L, request, 7L);

        verify(plazoletaDtoMapper).toDishUpdateModel(request);
        verify(catalogUseCasePort).updateDish(eq(10L), eq(7L), eq(patch));
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
        OrderResponse expected = new OrderResponse(200L, 8L, 30L, List.of(), "LISTO", model.getFechaCreacion(), 20L);
        when(orderUseCasePort.markReady(200L, 20L)).thenReturn(model);
        when(plazoletaDtoMapper.toOrderResponse(model)).thenReturn(expected);

        var response = orderHandler.markReady(200L, 20L);

        assertEquals(200L, response.idPedido());
        assertEquals("LISTO", response.estadoActual());
        verify(orderUseCasePort).markReady(200L, 20L);
        verify(plazoletaDtoMapper).toOrderResponse(model);
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
