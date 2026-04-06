package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.exception.AccessDeniedException;
import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.exception.ResourceNotFoundException;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> catalogUseCase.createRestaurant(model));
        assertEquals("El idPropietario no corresponde a un usuario con rol PROPIETARIO", ex.getMessage());
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

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> catalogUseCase.createDish(dishModel, 99L));
        assertEquals("No puedes crear platos en restaurantes de otro propietario", ex.getMessage());
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
    void assignEmployeeShouldFailWhenRestaurantNotFound() {
        when(catalogPersistencePort.findRestaurantById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(com.pragma.powerup.plazoleta.domain.exception.ResourceNotFoundException.class,
                () -> catalogUseCase.assignEmployeeToRestaurant(99L, 500L, 1L));
        assertEquals("Restaurante no existe", ex.getMessage());
    }

    @Test
    void assignEmployeeShouldFailWhenOwnerDoesNotOwnRestaurant() {
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(10L).idPropietario(2L).build();
        when(catalogPersistencePort.findRestaurantById(10L)).thenReturn(Optional.of(restaurant));

        var ex = assertThrows(com.pragma.powerup.plazoleta.domain.exception.AccessDeniedException.class,
                () -> catalogUseCase.assignEmployeeToRestaurant(10L, 500L, 1L));
        assertEquals("No puedes asignar empleados en restaurantes de otro propietario", ex.getMessage());
    }

    @Test
    void assignEmployeeShouldFailWhenUserIsNotEmpleado() {
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(10L).idPropietario(1L).build();
        when(catalogPersistencePort.findRestaurantById(10L)).thenReturn(Optional.of(restaurant));
        when(usuariosValidationPort.isEmpleado(500L)).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> catalogUseCase.assignEmployeeToRestaurant(10L, 500L, 1L));
        assertEquals("El idEmpleado no corresponde a un usuario con rol EMPLEADO", ex.getMessage());
    }

    @Test
    void assignEmployeeShouldFailWhenAlreadyAssigned() {
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(10L).idPropietario(1L).build();
        when(catalogPersistencePort.findRestaurantById(10L)).thenReturn(Optional.of(restaurant));
        when(usuariosValidationPort.isEmpleado(500L)).thenReturn(true);
        when(catalogPersistencePort.existsEmployeeAssignment(500L, 10L)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> catalogUseCase.assignEmployeeToRestaurant(10L, 500L, 1L));
        assertEquals("El empleado ya esta asignado a este restaurante", ex.getMessage());
    }

    @Test
    void assignEmployeeShouldSucceedWhenValid() {
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(10L).idPropietario(1L).build();
        when(catalogPersistencePort.findRestaurantById(10L)).thenReturn(Optional.of(restaurant));
        when(usuariosValidationPort.isEmpleado(500L)).thenReturn(true);
        when(catalogPersistencePort.existsEmployeeAssignment(500L, 10L)).thenReturn(false);
        when(catalogPersistencePort.saveEmployeeAssignment(500L, 10L)).thenReturn(77L);

        Long id = catalogUseCase.assignEmployeeToRestaurant(10L, 500L, 1L);
        assertEquals(77L, id);
        verify(catalogPersistencePort).saveEmployeeAssignment(500L, 10L);
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

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> catalogUseCase.updateDish(9L, 30L, update));
        assertEquals("El precio debe ser mayor a 0", ex.getMessage());
        verify(catalogPersistencePort, never()).updateDish(any());
    }

    // ========================
    // HU 2 — createRestaurant validaciones
    // ========================

    @Test
    void createRestaurantShouldSucceedWhenAllValid() {
        when(usuariosValidationPort.isPropietario(10L)).thenReturn(true);
        when(catalogPersistencePort.createRestaurant(any())).thenReturn(1L);

        RestaurantModel model = RestaurantModel.builder()
                .nombre("Restaurante Alfa")
                .nit("123456789")
                .direccion("Calle 10")
                .telefono("+573001112233")
                .urlLogo("http://logo.png")
                .idPropietario(10L)
                .build();

        Long id = catalogUseCase.createRestaurant(model);
        assertEquals(1L, id);
        verify(catalogPersistencePort).createRestaurant(model);
    }

    @Test
    void createRestaurantShouldFailWhenNitIsNotNumeric() {
        RestaurantModel model = RestaurantModel.builder()
                .nombre("Restaurante Beta")
                .nit("ABC123")
                .direccion("Calle 20")
                .telefono("+573001234567")
                .urlLogo("http://logo.png")
                .idPropietario(10L)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> catalogUseCase.createRestaurant(model));
        assertEquals("El NIT debe ser numerico", ex.getMessage());
    }

    @Test
    void createRestaurantShouldFailWhenPhoneInvalid() {
        RestaurantModel model = RestaurantModel.builder()
                .nombre("Restaurante Gamma")
                .nit("999888")
                .direccion("Calle 30")
                .telefono("no-es-telefono")
                .urlLogo("http://logo.png")
                .idPropietario(10L)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> catalogUseCase.createRestaurant(model));
        assertEquals("Telefono invalido", ex.getMessage());
    }

    @Test
    void createRestaurantShouldFailWhenNameIsOnlyNumbers() {
        RestaurantModel model = RestaurantModel.builder()
                .nombre("12345")
                .nit("999888")
                .direccion("Calle 40")
                .telefono("+573001112233")
                .urlLogo("http://logo.png")
                .idPropietario(10L)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> catalogUseCase.createRestaurant(model));
        assertEquals("Nombre de restaurante invalido", ex.getMessage());
    }

    // ========================
    // HU 4 — updateDish happy path + ownership
    // ========================

    @Test
    void updateDishShouldSucceedWhenValid() {
        DishModel currentDish = DishModel.builder()
                .id(9L).idRestaurante(5L).nombre("Pasta")
                .precio(BigDecimal.TEN).descripcion("original")
                .urlImagen("img").categoria("ITALIANA").activo(true)
                .build();
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(5L).idPropietario(30L).build();
        when(catalogPersistencePort.findDishById(9L)).thenReturn(Optional.of(currentDish));
        when(catalogPersistencePort.findRestaurantById(5L)).thenReturn(Optional.of(restaurant));

        DishModel update = DishModel.builder()
                .precio(new BigDecimal("15000"))
                .descripcion("nueva descripcion")
                .build();

        catalogUseCase.updateDish(9L, 30L, update);

        verify(catalogPersistencePort).updateDish(argThat(d ->
                d.getPrecio().equals(new BigDecimal("15000"))
                        && d.getDescripcion().equals("nueva descripcion")
                        && d.getNombre().equals("Pasta")
                        && d.getCategoria().equals("ITALIANA")
        ));
    }

    @Test
    void updateDishShouldFailWhenOwnerDoesNotOwnRestaurant() {
        DishModel currentDish = DishModel.builder()
                .id(9L).idRestaurante(5L).nombre("Pasta")
                .precio(BigDecimal.TEN).activo(true)
                .build();
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(5L).idPropietario(30L).build();
        when(catalogPersistencePort.findDishById(9L)).thenReturn(Optional.of(currentDish));
        when(catalogPersistencePort.findRestaurantById(5L)).thenReturn(Optional.of(restaurant));

        DishModel update = DishModel.builder()
                .precio(BigDecimal.TEN).descripcion("hack").build();

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> catalogUseCase.updateDish(9L, 99L, update));
        assertTrue(ex.getMessage().contains("otro restaurante"));
        verify(catalogPersistencePort, never()).updateDish(any());
    }

    @Test
    void updateDishShouldFailWhenDishNotFound() {
        when(catalogPersistencePort.findDishById(99L)).thenReturn(Optional.empty());

        DishModel update = DishModel.builder()
                .precio(BigDecimal.TEN).descripcion("x").build();

        assertThrows(ResourceNotFoundException.class,
                () -> catalogUseCase.updateDish(99L, 30L, update));
    }

    // ========================
    // HU 7 — setDishActive (soft delete / habilitar-deshabilitar)
    // ========================

    @Test
    void setDishActiveShouldDisableDish() {
        DishModel dish = DishModel.builder()
                .id(7L).idRestaurante(5L).nombre("Plato")
                .precio(BigDecimal.TEN).descripcion("d").urlImagen("img")
                .categoria("CAT").activo(true)
                .build();
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(5L).idPropietario(30L).build();
        when(catalogPersistencePort.findDishById(7L)).thenReturn(Optional.of(dish));
        when(catalogPersistencePort.findRestaurantById(5L)).thenReturn(Optional.of(restaurant));

        catalogUseCase.setDishActive(7L, 30L, false);

        verify(catalogPersistencePort).updateDish(argThat(d ->
                Boolean.FALSE.equals(d.getActivo()) && d.getId().equals(7L)));
    }

    @Test
    void setDishActiveShouldEnableDish() {
        DishModel dish = DishModel.builder()
                .id(7L).idRestaurante(5L).nombre("Plato")
                .precio(BigDecimal.TEN).descripcion("d").urlImagen("img")
                .categoria("CAT").activo(false)
                .build();
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(5L).idPropietario(30L).build();
        when(catalogPersistencePort.findDishById(7L)).thenReturn(Optional.of(dish));
        when(catalogPersistencePort.findRestaurantById(5L)).thenReturn(Optional.of(restaurant));

        catalogUseCase.setDishActive(7L, 30L, true);

        verify(catalogPersistencePort).updateDish(argThat(d ->
                Boolean.TRUE.equals(d.getActivo()) && d.getId().equals(7L)));
    }

    @Test
    void setDishActiveShouldFailWhenOwnerDoesNotOwnRestaurant() {
        DishModel dish = DishModel.builder()
                .id(7L).idRestaurante(5L).nombre("Plato")
                .precio(BigDecimal.TEN).activo(true)
                .build();
        RestaurantModel restaurant = RestaurantModel.builder()
                .id(5L).idPropietario(30L).build();
        when(catalogPersistencePort.findDishById(7L)).thenReturn(Optional.of(dish));
        when(catalogPersistencePort.findRestaurantById(5L)).thenReturn(Optional.of(restaurant));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> catalogUseCase.setDishActive(7L, 99L, false));
        assertTrue(ex.getMessage().contains("otro restaurante"));
        verify(catalogPersistencePort, never()).updateDish(any());
    }

    @Test
    void setDishActiveShouldFailWhenDishNotFound() {
        when(catalogPersistencePort.findDishById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> catalogUseCase.setDishActive(99L, 30L, false));
    }

    // ========================
    // HU 9 — listRestaurants
    // ========================

    @Test
    void listRestaurantsShouldDelegateToPort() {
        PaginationParams params = new PaginationParams(0, 10);
        PageResult<RestaurantModel> expected = new PageResult<>(
                List.of(RestaurantModel.builder().id(1L).nombre("A").urlLogo("logo").build()),
                0, 10, 1, 1);
        when(catalogPersistencePort.listRestaurants(params)).thenReturn(expected);

        PageResult<RestaurantModel> result = catalogUseCase.listRestaurants(params);

        assertEquals(1, result.totalElements());
        assertEquals("A", result.content().get(0).getNombre());
        verify(catalogPersistencePort).listRestaurants(params);
    }

    @Test
    void listRestaurantsShouldReturnEmptyPageWhenNoRestaurants() {
        PaginationParams params = new PaginationParams(0, 10);
        PageResult<RestaurantModel> empty = new PageResult<>(List.of(), 0, 10, 0, 0);
        when(catalogPersistencePort.listRestaurants(params)).thenReturn(empty);

        PageResult<RestaurantModel> result = catalogUseCase.listRestaurants(params);

        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
    }

    // ========================
    // HU 10 — listDishes
    // ========================

    @Test
    void listDishesShouldDelegateToPortWithCategory() {
        PaginationParams params = new PaginationParams(0, 10);
        PageResult<DishModel> expected = new PageResult<>(
                List.of(DishModel.builder().id(1L).nombre("Plato").activo(true).build()),
                0, 10, 1, 1);
        when(catalogPersistencePort.listActiveDishes(5L, "ITALIANA", params)).thenReturn(expected);

        PageResult<DishModel> result = catalogUseCase.listDishes(5L, "ITALIANA", params);

        assertEquals(1, result.totalElements());
        verify(catalogPersistencePort).listActiveDishes(5L, "ITALIANA", params);
    }

    @Test
    void listDishesShouldDelegateToPortWithoutCategory() {
        PaginationParams params = new PaginationParams(0, 5);
        PageResult<DishModel> expected = new PageResult<>(List.of(), 0, 5, 0, 0);
        when(catalogPersistencePort.listActiveDishes(5L, null, params)).thenReturn(expected);

        PageResult<DishModel> result = catalogUseCase.listDishes(5L, null, params);

        assertEquals(0, result.totalElements());
        verify(catalogPersistencePort).listActiveDishes(5L, null, params);
    }
}
