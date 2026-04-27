package com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class IDishEntityMapperTest {

    private IDishEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new IDishEntityMapperImpl();
    }

    // ── toDishEntity ──────────────────────────────────────────────────────────

    @Test
    void toDishEntityMapsAllFieldsAndIgnoresId() {
        DishModel model = DishModel.builder()
                .id(99L).idRestaurante(5L).nombre("Bandeja Paisa")
                .precio(BigDecimal.valueOf(25000)).descripcion("Completa")
                .urlImagen("https://img.png").categoria("Típica").activo(true)
                .build();
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(5L);

        DishEntity entity = mapper.toDishEntity(model, restaurant);

        assertNull(entity.getId());
        assertEquals("Bandeja Paisa", entity.getNombre());
        assertEquals(0, entity.getPrecio().compareTo(BigDecimal.valueOf(25000)));
        assertEquals("Completa", entity.getDescripcion());
        assertEquals("https://img.png", entity.getUrlImagen());
        assertEquals("Típica", entity.getCategoria());
        assertTrue(entity.getActivo());
        assertEquals(5L, entity.getRestaurant().getId());
    }

    // ── toDishModel ───────────────────────────────────────────────────────────

    @Test
    void toDishModelMapsAllFieldsIncludingRestaurantId() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(7L);

        DishEntity entity = new DishEntity();
        entity.setId(3L);
        entity.setNombre("Ajiaco");
        entity.setPrecio(BigDecimal.valueOf(18000));
        entity.setDescripcion("Con papa criolla");
        entity.setUrlImagen("https://ajiaco.png");
        entity.setCategoria("Sopas");
        entity.setActivo(true);
        entity.setRestaurant(restaurant);

        DishModel model = mapper.toDishModel(entity);

        assertEquals(3L, model.getId());
        assertEquals(7L, model.getIdRestaurante());
        assertEquals("Ajiaco", model.getNombre());
        assertEquals(0, model.getPrecio().compareTo(BigDecimal.valueOf(18000)));
        assertEquals("Con papa criolla", model.getDescripcion());
        assertEquals("https://ajiaco.png", model.getUrlImagen());
        assertEquals("Sopas", model.getCategoria());
        assertTrue(model.getActivo());
    }

    @Test
    void toDishModelReturnsNullWhenEntityIsNull() {
        assertNull(mapper.toDishModel(null));
    }

    // ── updateDishEntity ──────────────────────────────────────────────────────

    @Test
    void updateDishEntityUpdatesPrecioDescripcionAndActivo() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);

        DishEntity entity = new DishEntity();
        entity.setId(10L);
        entity.setNombre("Pollo asado");
        entity.setPrecio(BigDecimal.valueOf(12000));
        entity.setDescripcion("Original");
        entity.setUrlImagen("https://pollo.png");
        entity.setCategoria("Carnes");
        entity.setActivo(true);
        entity.setRestaurant(restaurant);

        DishModel update = DishModel.builder()
                .id(10L).idRestaurante(1L).nombre("Pollo asado")
                .precio(BigDecimal.valueOf(15000)).descripcion("Mejorada")
                .urlImagen("https://pollo.png").categoria("Carnes").activo(false)
                .build();

        mapper.updateDishEntity(update, entity);

        assertEquals(10L, entity.getId());
        assertEquals("Pollo asado", entity.getNombre());
        assertEquals(0, entity.getPrecio().compareTo(BigDecimal.valueOf(15000)));
        assertEquals("Mejorada", entity.getDescripcion());
        assertEquals("https://pollo.png", entity.getUrlImagen());
        assertEquals("Carnes", entity.getCategoria());
        assertFalse(entity.getActivo());
        assertEquals(1L, entity.getRestaurant().getId());
    }

    @Test
    void updateDishEntityPreservesUnchangedFields() {
        DishEntity entity = new DishEntity();
        entity.setId(5L);
        entity.setNombre("Tamal");
        entity.setPrecio(BigDecimal.valueOf(8000));
        entity.setDescripcion("Tradicional");
        entity.setCategoria("Típica");
        entity.setActivo(true);

        DishModel update = DishModel.builder()
                .id(5L).precio(BigDecimal.valueOf(9000)).descripcion("Mejorado")
                .activo(true).build();

        mapper.updateDishEntity(update, entity);

        assertEquals(5L, entity.getId());
        assertEquals("Tamal", entity.getNombre());
        assertEquals("Típica", entity.getCategoria());
        assertEquals(0, entity.getPrecio().compareTo(BigDecimal.valueOf(9000)));
        assertEquals("Mejorado", entity.getDescripcion());
    }
}
