package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogUseCaseTest {

    private CatalogPersistencePort catalogPersistencePort;
    private UsuariosValidationPort usuariosValidationPort;
    private CatalogUseCase catalogUseCase;

    @BeforeEach
    void setUp() {
        catalogPersistencePort = mock(CatalogPersistencePort.class);
        usuariosValidationPort = mock(UsuariosValidationPort.class);
        catalogUseCase = new CatalogUseCase(catalogPersistencePort, usuariosValidationPort);
    }

    @Test
    void createRestaurantShouldFailWhenOwnerIsNotPropietario() {
        when(usuariosValidationPort.isPropietario(10L)).thenReturn(false);

        RestaurantModel model = RestaurantModel.builder()
                .nombre("Restaurante Uno")
                .nit("123456")
                .direccion("Calle 1")
                .telefono("+573001112233")
                .urlLogo("http://logo")
                .idPropietario(10L)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> catalogUseCase.createRestaurant(model));
        assertEquals(400, ex.getStatus().value());
        verify(catalogPersistencePort, never()).createRestaurant(any());
    }

    @Test
    void createDishShouldFailWhenOwnerIsNotRestaurantOwner() {
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(5L)
                .idPropietario(30L)
                .build();
        when(catalogPersistencePort.findRestaurantById(5L)).thenReturn(Optional.of(restaurant));

        DishModel dishModel = DishModel.builder()
                .idRestaurante(5L)
                .precio(BigDecimal.TEN)
                .nombre("Pasta")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> catalogUseCase.createDish(dishModel, 99L));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void createDishShouldSetActiveTrueByDefault() {
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(5L)
                .idPropietario(30L)
                .build();
        when(catalogPersistencePort.findRestaurantById(5L)).thenReturn(Optional.of(restaurant));
        when(catalogPersistencePort.createDish(any())).thenReturn(77L);

        DishModel dishModel = DishModel.builder()
                .idRestaurante(5L)
                .precio(BigDecimal.TEN)
                .nombre("Pasta")
                .descripcion("desc")
                .urlImagen("img")
                .categoria("CAT")
                .build();

        Long createdId = catalogUseCase.createDish(dishModel, 30L);
        assertEquals(77L, createdId);
        verify(catalogPersistencePort).createDish(argThat(model -> Boolean.TRUE.equals(model.getActivo())));
    }

    @Test
    void updateDishShouldFailWhenPriceIsNotPositive() {
        DishModel currentDish = DishModel.builder()
                .id(9L)
                .idRestaurante(5L)
                .nombre("Pasta")
                .precio(BigDecimal.TEN)
                .activo(true)
                .build();
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(5L)
                .idPropietario(30L)
                .build();
        when(catalogPersistencePort.findDishById(9L)).thenReturn(Optional.of(currentDish));
        when(catalogPersistencePort.findRestaurantById(5L)).thenReturn(Optional.of(restaurant));

        DishModel update = DishModel.builder()
                .precio(BigDecimal.ZERO)
                .descripcion("nueva")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> catalogUseCase.updateDish(9L, 30L, update));
        assertEquals(400, ex.getStatus().value());
        verify(catalogPersistencePort, never()).updateDish(any());
    }
}
