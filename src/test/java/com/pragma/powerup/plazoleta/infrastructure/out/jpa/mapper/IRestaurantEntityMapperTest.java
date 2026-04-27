package com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper;

import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IRestaurantEntityMapperTest {

    private IRestaurantEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new IRestaurantEntityMapperImpl();
    }

    @Test
    void toRestaurantEntityMapsAllFields() {
        RestaurantModel model = RestaurantModel.builder()
                .id(1L).nombre("El Corral").nit("900123456")
                .direccion("Calle 10 # 5-20").telefono("+573001234567")
                .urlLogo("https://logo.png").idPropietario(42L)
                .build();

        RestaurantEntity entity = mapper.toRestaurantEntity(model);

        assertEquals(1L, entity.getId());
        assertEquals("El Corral", entity.getNombre());
        assertEquals("900123456", entity.getNit());
        assertEquals("Calle 10 # 5-20", entity.getDireccion());
        assertEquals("+573001234567", entity.getTelefono());
        assertEquals("https://logo.png", entity.getUrlLogo());
        assertEquals(42L, entity.getIdPropietario());
    }

    @Test
    void toRestaurantModelMapsAllFields() {
        RestaurantEntity entity = new RestaurantEntity();
        entity.setId(2L);
        entity.setNombre("La Fogata");
        entity.setNit("800987654");
        entity.setDireccion("Av. Siempreviva 742");
        entity.setTelefono("+573109876543");
        entity.setUrlLogo("https://fogata.png");
        entity.setIdPropietario(10L);

        RestaurantModel model = mapper.toRestaurantModel(entity);

        assertEquals(2L, model.getId());
        assertEquals("La Fogata", model.getNombre());
        assertEquals("800987654", model.getNit());
        assertEquals("Av. Siempreviva 742", model.getDireccion());
        assertEquals("+573109876543", model.getTelefono());
        assertEquals("https://fogata.png", model.getUrlLogo());
        assertEquals(10L, model.getIdPropietario());
    }

    @Test
    void toRestaurantModelReturnsNullWhenEntityIsNull() {
        assertNull(mapper.toRestaurantModel(null));
    }

    @Test
    void toRestaurantEntityReturnsNullWhenModelIsNull() {
        assertNull(mapper.toRestaurantEntity(null));
    }
}
