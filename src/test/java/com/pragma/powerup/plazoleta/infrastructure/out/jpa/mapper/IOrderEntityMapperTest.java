package com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper;

import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.EstadoPedido;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderItemEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IOrderEntityMapperTest {

    private IOrderEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new IOrderEntityMapperImpl();
    }

    // ── toOrderItemModel ──────────────────────────────────────────────────────

    @Test
    void toOrderItemModelMapsIdPlatoNombrePlatoAndCantidad() {
        DishEntity dish = new DishEntity();
        dish.setId(10L);
        dish.setNombre("Arroz con leche");

        OrderItemEntity item = new OrderItemEntity();
        item.setDish(dish);
        item.setCantidad(3);

        OrderItemModel model = mapper.toOrderItemModel(item);

        assertEquals(10L, model.getIdPlato());
        assertEquals("Arroz con leche", model.getNombrePlato());
        assertEquals(3, model.getCantidad());
    }

    @Test
    void toOrderItemModelReturnsNullWhenItemIsNull() {
        assertNull(mapper.toOrderItemModel(null));
    }

    // ── toOrderModel ──────────────────────────────────────────────────────────

    @Test
    void toOrderModelMapsAllScalarFields() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(5L);

        LocalDateTime created = LocalDateTime.of(2025, 1, 10, 12, 0);
        LocalDateTime delivered = LocalDateTime.of(2025, 1, 10, 12, 30);

        OrderEntity entity = new OrderEntity();
        entity.setId(100L);
        entity.setRestaurant(restaurant);
        entity.setIdCliente(20L);
        entity.setTelefonoCliente("+573001234567");
        entity.setEstado(EstadoPedido.EN_PREPARACION);
        entity.setFechaCreacion(created);
        entity.setFechaEntrega(delivered);
        entity.setPinSeguridad("ABC123");
        entity.setIdEmpleadoAsignado(30L);

        OrderModel model = mapper.toOrderModel(entity);

        assertEquals(100L, model.getId());
        assertEquals(5L, model.getIdRestaurante());
        assertEquals(20L, model.getIdCliente());
        assertEquals("+573001234567", model.getTelefonoCliente());
        assertEquals(EstadoPedidoModel.EN_PREPARACION, model.getEstado());
        assertEquals(created, model.getFechaCreacion());
        assertEquals(delivered, model.getFechaEntrega());
        assertEquals("ABC123", model.getPinSeguridad());
        assertEquals(30L, model.getIdEmpleadoAsignado());
    }

    @Test
    void toOrderModelMapsItemsListCorrectly() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        DishEntity dish1 = new DishEntity();
        dish1.setId(11L);
        dish1.setNombre("Pizza");

        DishEntity dish2 = new DishEntity();
        dish2.setId(22L);
        dish2.setNombre("Pasta");

        OrderItemEntity item1 = new OrderItemEntity();
        item1.setDish(dish1);
        item1.setCantidad(2);

        OrderItemEntity item2 = new OrderItemEntity();
        item2.setDish(dish2);
        item2.setCantidad(1);

        OrderEntity entity = new OrderEntity();
        entity.setId(200L);
        entity.setRestaurant(restaurant);
        entity.setIdCliente(50L);
        entity.setTelefonoCliente("+573009876543");
        entity.setEstado(EstadoPedido.PENDIENTE);
        entity.setFechaCreacion(LocalDateTime.now());
        entity.getItems().addAll(List.of(item1, item2));

        OrderModel model = mapper.toOrderModel(entity);

        assertEquals(2, model.getItems().size());
        assertEquals(11L, model.getItems().get(0).getIdPlato());
        assertEquals("Pizza", model.getItems().get(0).getNombrePlato());
        assertEquals(2, model.getItems().get(0).getCantidad());
        assertEquals(22L, model.getItems().get(1).getIdPlato());
        assertEquals("Pasta", model.getItems().get(1).getNombrePlato());
        assertEquals(1, model.getItems().get(1).getCantidad());
    }

    @Test
    void toOrderModelMapsEmptyItemsListWhenNoItems() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        OrderEntity entity = new OrderEntity();
        entity.setId(300L);
        entity.setRestaurant(restaurant);
        entity.setIdCliente(60L);
        entity.setTelefonoCliente("+573000000000");
        entity.setEstado(EstadoPedido.CANCELADO);
        entity.setFechaCreacion(LocalDateTime.now());

        OrderModel model = mapper.toOrderModel(entity);

        assertNotNull(model.getItems());
        assertTrue(model.getItems().isEmpty());
    }

    @Test
    void toOrderModelMapsAllEstadoPedidoValues() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        for (EstadoPedido estado : EstadoPedido.values()) {
            OrderEntity entity = new OrderEntity();
            entity.setId(1L);
            entity.setRestaurant(restaurant);
            entity.setIdCliente(1L);
            entity.setTelefonoCliente("+57300");
            entity.setEstado(estado);
            entity.setFechaCreacion(LocalDateTime.now());

            OrderModel model = mapper.toOrderModel(entity);

            assertEquals(EstadoPedidoModel.valueOf(estado.name()), model.getEstado());
        }
    }

    @Test
    void toOrderModelReturnsNullWhenEntityIsNull() {
        assertNull(mapper.toOrderModel(null));
    }
}
