package com.pragma.powerup.plazoleta.domain.model;

import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RestaurantModelTest {

    // ── NIT ────────────────────────────────────────────────────────────────────

    @Test
    void buildShouldSucceedWhenNitIsAllDigits() {
        RestaurantModel model = RestaurantModel.builder()
                .nombre("Mi Restaurante")
                .nit("123456789")
                .telefono("+573001112233")
                .build();
        assertEquals("123456789", model.getNit());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC123", "12 34", "nit-123", "123.456"})
    void buildShouldFailWhenNitContainsNonDigits(String invalidNit) {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                RestaurantModel.builder()
                        .nombre("Mi Restaurante")
                        .nit(invalidNit)
                        .telefono("+573001112233")
                        .build());
        assertEquals("El NIT debe ser numerico", ex.getMessage());
    }

    @Test
    void buildShouldSkipNitValidationWhenNitIsNull() {
        assertDoesNotThrow(() -> RestaurantModel.builder().id(1L).build());
    }

    // ── Teléfono ───────────────────────────────────────────────────────────────

    @Test
    void buildShouldSucceedForPhoneWithPlusPrefix() {
        RestaurantModel model = RestaurantModel.builder()
                .telefono("+573005698325")
                .build();
        assertEquals("+573005698325", model.getTelefono());
    }

    @Test
    void buildShouldSucceedForPhoneWithoutPlusPrefix() {
        RestaurantModel model = RestaurantModel.builder()
                .telefono("3001234567")
                .build();
        assertEquals("3001234567", model.getTelefono());
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-es-telefono", "++573001234567", "12345678901234", "abc"})
    void buildShouldFailWhenPhoneIsInvalid(String invalidPhone) {
        assertThrows(BusinessRuleException.class, () ->
                RestaurantModel.builder()
                        .nombre("Restaurante")
                        .nit("123")
                        .telefono(invalidPhone)
                        .build());
    }

    @Test
    void buildShouldSkipPhoneValidationWhenPhoneIsNull() {
        assertDoesNotThrow(() -> RestaurantModel.builder().id(2L).build());
    }

    // ── Nombre ─────────────────────────────────────────────────────────────────

    @Test
    void buildShouldSucceedWhenNameContainsMixedChars() {
        RestaurantModel model = RestaurantModel.builder()
                .nombre("Restaurante 123")
                .build();
        assertEquals("Restaurante 123", model.getNombre());
    }

    @Test
    void buildShouldSucceedWhenNameIsAlphabetic() {
        assertDoesNotThrow(() -> RestaurantModel.builder().nombre("El Buen Sabor").build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "0", "9999"})
    void buildShouldFailWhenNameIsOnlyNumbers(String onlyNumbers) {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                RestaurantModel.builder()
                        .nombre(onlyNumbers)
                        .nit("999")
                        .telefono("+573001112233")
                        .build());
        assertEquals("Nombre de restaurante invalido", ex.getMessage());
    }

    @Test
    void buildShouldSkipNameValidationWhenNameIsNull() {
        assertDoesNotThrow(() -> RestaurantModel.builder().id(3L).build());
    }

    // ── Construcción válida completa ───────────────────────────────────────────

    @Test
    void buildShouldSucceedWithAllValidFields() {
        RestaurantModel model = RestaurantModel.builder()
                .id(1L)
                .nombre("Restaurante Alfa")
                .nit("900123456")
                .direccion("Calle 10 #5-20")
                .telefono("+573001112233")
                .urlLogo("https://logo.png")
                .idPropietario(99L)
                .build();

        assertEquals(1L, model.getId());
        assertEquals("Restaurante Alfa", model.getNombre());
        assertEquals("900123456", model.getNit());
        assertEquals("+573001112233", model.getTelefono());
        assertEquals(99L, model.getIdPropietario());
    }
}
