package com.pragma.powerup.plazoleta.application.mapper;

import com.pragma.powerup.plazoleta.application.dto.request.CreateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateOrderRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.application.dto.request.UpdateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.response.DishResponse;
import com.pragma.powerup.plazoleta.application.dto.response.EficienciaResponse;
import com.pragma.powerup.plazoleta.application.dto.response.OrderResponse;
import com.pragma.powerup.plazoleta.application.dto.response.RestaurantCardResponse;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyEmployeeModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IPlazoletaDtoMapperTest {

    private IPlazoletaDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new IPlazoletaDtoMapperImpl();
    }

    // ── Peticiones a modelos ──────────────────────────────────────────────────

    @Test
    void toRestaurantModelMapsRequestAndLeavesIdNull() {
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "R1", "999", "Dir", "+573001112223", "https://logo", 40L);

        RestaurantModel model = mapper.toRestaurantModel(request);

        assertNull(model.getId());
        assertEquals("R1", model.getNombre());
        assertEquals("999", model.getNit());
        assertEquals("Dir", model.getDireccion());
        assertEquals("+573001112223", model.getTelefono());
        assertEquals("https://logo", model.getUrlLogo());
        assertEquals(40L, model.getIdPropietario());
    }

    @Test
    void toDishModelMapsRequestAndLeavesIdAndActivoNull() {
        CreateDishRequest request = new CreateDishRequest(
                "Arroz", BigDecimal.valueOf(12.5), "Con pollo", "https://img", "Otros", 9L);

        DishModel model = mapper.toDishModel(request);

        assertNull(model.getId());
        assertNull(model.getActivo());
        assertEquals("Arroz", model.getNombre());
        assertEquals(0, model.getPrecio().compareTo(BigDecimal.valueOf(12.5)));
        assertEquals("Con pollo", model.getDescripcion());
        assertEquals("https://img", model.getUrlImagen());
        assertEquals("Otros", model.getCategoria());
        assertEquals(9L, model.getIdRestaurante());
    }

    @Test
    void toDishUpdateModelMapsOnlyPriceAndDescription() {
        UpdateDishRequest request = new UpdateDishRequest(BigDecimal.valueOf(20), "Nueva desc");

        DishModel model = mapper.toDishUpdateModel(request);

        assertNull(model.getId());
        assertNull(model.getIdRestaurante());
        assertNull(model.getNombre());
        assertNull(model.getUrlImagen());
        assertNull(model.getCategoria());
        assertNull(model.getActivo());
        assertEquals(0, model.getPrecio().compareTo(BigDecimal.valueOf(20)));
        assertEquals("Nueva desc", model.getDescripcion());
    }

    @Test
    void nullRequestsMapToNull() {
        assertNull(mapper.toRestaurantModel(null));
        assertNull(mapper.toDishModel(null));
        assertNull(mapper.toDishUpdateModel(null));
    }

    @Test
    void toOrderItemModelMapsIdPlatoAndCantidadAndIgnoresNombrePlato() {
        CreateOrderRequest.OrderDish orderDish = new CreateOrderRequest.OrderDish(5L, 3);

        OrderItemModel model = mapper.toOrderItemModel(orderDish);

        assertEquals(5L, model.getIdPlato());
        assertEquals(3, model.getCantidad());
        assertNull(model.getNombrePlato());
    }

    @Test
    void toOrderItemsExtractsPlatosFromRequest() {
        CreateOrderRequest request = new CreateOrderRequest(1L, "3001234567",
                List.of(new CreateOrderRequest.OrderDish(10L, 2),
                        new CreateOrderRequest.OrderDish(20L, 1)));

        List<OrderItemModel> items = mapper.toOrderItems(request);

        assertEquals(2, items.size());
        assertEquals(10L, items.get(0).getIdPlato());
        assertEquals(2, items.get(0).getCantidad());
        assertEquals(20L, items.get(1).getIdPlato());
        assertEquals(1, items.get(1).getCantidad());
    }

    @Test
    void toOrderItemsReturnsEmptyListWhenPlatosIsNull() {
        CreateOrderRequest request = new CreateOrderRequest(1L, "3001234567", null);

        List<OrderItemModel> items = mapper.toOrderItems(request);

        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void toOrderItemsReturnsEmptyListWhenRequestIsNull() {
        List<OrderItemModel> items = mapper.toOrderItems(null);

        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    // ── Modelos a respuestas ──────────────────────────────────────────────────

    @Test
    void toRestaurantCardResponseMapsAllFields() {
        RestaurantModel model = RestaurantModel.builder()
                .id(7L).nombre("La Fogata").urlLogo("https://logo.png").build();

        RestaurantCardResponse response = mapper.toRestaurantCardResponse(model);

        assertEquals(7L, response.id());
        assertEquals("La Fogata", response.nombre());
        assertEquals("https://logo.png", response.urlLogo());
    }

    @Test
    void toDishResponseMapsAllFields() {
        DishModel model = DishModel.builder()
                .id(3L).nombre("Pasta").precio(new BigDecimal("15000"))
                .descripcion("Al dente").urlImagen("https://img.png")
                .categoria("Italiana").activo(true).build();

        DishResponse response = mapper.toDishResponse(model);

        assertEquals(3L, response.id());
        assertEquals("Pasta", response.nombre());
        assertEquals(0, response.precio().compareTo(new BigDecimal("15000")));
        assertEquals("Al dente", response.descripcion());
        assertEquals("https://img.png", response.urlImagen());
        assertEquals("Italiana", response.categoria());
        assertTrue(response.activo());
    }

    @Test
    void toOrderResponseMapsFieldsIncludingRenamesAndStateToString() {
        LocalDateTime created = LocalDateTime.now().minusMinutes(5);
        OrderItemModel item = OrderItemModel.builder()
                .idPlato(1L).nombrePlato("Arroz").cantidad(2).build();
        OrderModel model = OrderModel.builder()
                .id(100L).idRestaurante(5L).idCliente(10L)
                .estado(EstadoPedidoModel.EN_PREPARACION)
                .fechaCreacion(created).idEmpleadoAsignado(20L)
                .items(List.of(item)).build();

        OrderResponse response = mapper.toOrderResponse(model);

        assertEquals(100L, response.idPedido());
        assertEquals(5L, response.idRestaurante());
        assertEquals(10L, response.idCliente());
        assertEquals("EN_PREPARACION", response.estadoActual());
        assertEquals(created, response.fechaCreacion());
        assertEquals(20L, response.idEmpleadoAsignado());
        assertEquals(1, response.lineas().size());
        assertEquals(1L, response.lineas().get(0).idPlato());
        assertEquals("Arroz", response.lineas().get(0).nombrePlato());
        assertEquals(2, response.lineas().get(0).cantidad());
    }

    @Test
    void toEficienciaResponseMapsRankingWithFieldRename() {
        OrderEfficiencyEmployeeModel employee = new OrderEfficiencyEmployeeModel(42L, 12.5);
        OrderEfficiencyModel model = new OrderEfficiencyModel(List.of(employee));

        EficienciaResponse response = mapper.toEficienciaResponse(model);

        assertEquals(1, response.ranking().size());
        assertEquals(42L, response.ranking().get(0).idEmpleadoAsignado());
        assertEquals(12.5, response.ranking().get(0).tiempoPromedioMinutos());
    }
}
