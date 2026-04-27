package com.pragma.powerup.plazoleta.domain.model;

import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DishModelTest {

    // ── Price construction validation ──────────────────────────────────────────

    @Test
    void buildShouldSucceedWhenPriceIsPositive() {
        DishModel dish = DishModel.builder()
                .id(1L)
                .idRestaurante(10L)
                .nombre("Bandeja Paisa")
                .precio(new BigDecimal("25000"))
                .activo(true)
                .build();
        assertEquals(new BigDecimal("25000"), dish.getPrecio());
    }

    @Test
    void buildShouldFailWhenPriceIsZero() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                DishModel.builder()
                        .nombre("Plato")
                        .precio(BigDecimal.ZERO)
                        .build());
        assertEquals("El precio debe ser mayor a 0", ex.getMessage());
    }

    @Test
    void buildShouldFailWhenPriceIsNegative() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                DishModel.builder()
                        .nombre("Plato")
                        .precio(new BigDecimal("-1"))
                        .build());
        assertEquals("El precio debe ser mayor a 0", ex.getMessage());
    }

    @Test
    void buildShouldSkipPriceValidationWhenPriceIsNull() {
        assertDoesNotThrow(() -> DishModel.builder().id(5L).idRestaurante(1L).activo(true).build());
    }

    // ── assertIsActive ─────────────────────────────────────────────────────────

    @Test
    void assertIsActiveShouldPassWhenDishIsActive() {
        DishModel dish = DishModel.builder()
                .id(1L).idRestaurante(1L).precio(BigDecimal.TEN).activo(true).build();
        assertDoesNotThrow(dish::assertIsActive);
    }

    @Test
    void assertIsActiveShouldFailWhenDishIsInactive() {
        DishModel dish = DishModel.builder()
                .id(1L).idRestaurante(1L).precio(BigDecimal.TEN).activo(false).build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, dish::assertIsActive);
        assertEquals("No se pueden pedir platos inactivos", ex.getMessage());
    }

    @Test
    void assertIsActiveShouldFailWhenActivoIsNull() {
        DishModel dish = DishModel.builder()
                .id(1L).idRestaurante(1L).precio(BigDecimal.TEN).activo(null).build();
        assertThrows(BusinessRuleException.class, dish::assertIsActive);
    }

    // ── assertBelongsToRestaurant ──────────────────────────────────────────────

    @Test
    void assertBelongsToRestaurantShouldPassWhenRestaurantMatches() {
        DishModel dish = DishModel.builder()
                .id(1L).idRestaurante(10L).precio(BigDecimal.TEN).activo(true).build();
        assertDoesNotThrow(() -> dish.assertBelongsToRestaurant(10L));
    }

    @Test
    void assertBelongsToRestaurantShouldFailWhenRestaurantDiffers() {
        DishModel dish = DishModel.builder()
                .id(1L).idRestaurante(10L).precio(BigDecimal.TEN).activo(true).build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> dish.assertBelongsToRestaurant(99L));
        assertEquals("Un pedido debe incluir platos de un solo restaurante", ex.getMessage());
    }
}
