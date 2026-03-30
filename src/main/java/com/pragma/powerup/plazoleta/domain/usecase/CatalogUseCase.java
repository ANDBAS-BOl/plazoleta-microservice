package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

public class CatalogUseCase implements CatalogUseCasePort {

    private final CatalogPersistencePort catalogPersistencePort;
    private final UsuariosValidationPort usuariosValidationPort;

    public CatalogUseCase(CatalogPersistencePort catalogPersistencePort,
                          UsuariosValidationPort usuariosValidationPort) {
        this.catalogPersistencePort = catalogPersistencePort;
        this.usuariosValidationPort = usuariosValidationPort;
    }

    @Override
    public Long createRestaurant(RestaurantModel restaurantModel) {
        if (!restaurantModel.getNit().matches("\\d+")) {
            throw badRequest("El NIT debe ser numerico");
        }
        if (!restaurantModel.getTelefono().matches("^\\+?\\d{1,13}$")) {
            throw badRequest("Telefono invalido");
        }
        if (restaurantModel.getNombre().matches("^\\d+$")) {
            throw badRequest("Nombre de restaurante invalido");
        }
        if (!usuariosValidationPort.isPropietario(restaurantModel.getIdPropietario())) {
            throw badRequest("El idPropietario no corresponde a un usuario con rol PROPIETARIO");
        }

        return catalogPersistencePort.createRestaurant(restaurantModel);
    }

    @Override
    public Long createDish(DishModel dishModel, Long ownerId) {
        RestaurantModel restaurant = catalogPersistencePort.findRestaurantById(dishModel.getIdRestaurante())
                .orElseThrow(() -> notFound("Restaurante no existe"));

        if (!restaurant.getIdPropietario().equals(ownerId)) {
            throw forbidden("No puedes crear platos en restaurantes de otro propietario");
        }
        if (dishModel.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("El precio debe ser mayor a 0");
        }

        DishModel dishToCreate = DishModel.builder()
                .idRestaurante(dishModel.getIdRestaurante())
                .nombre(dishModel.getNombre())
                .precio(dishModel.getPrecio())
                .descripcion(dishModel.getDescripcion())
                .urlImagen(dishModel.getUrlImagen())
                .categoria(dishModel.getCategoria())
                .activo(true)
                .build();
        return catalogPersistencePort.createDish(dishToCreate);
    }

    @Override
    public void updateDish(Long idDish, Long ownerId, DishModel dishModel) {
        DishModel currentDish = catalogPersistencePort.findDishById(idDish)
                .orElseThrow(() -> notFound("Plato no existe"));

        RestaurantModel restaurant = catalogPersistencePort.findRestaurantById(currentDish.getIdRestaurante())
                .orElseThrow(() -> notFound("Restaurante no existe"));

        if (!restaurant.getIdPropietario().equals(ownerId)) {
            throw forbidden("No puedes modificar platos de otro restaurante");
        }
        if (dishModel.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("El precio debe ser mayor a 0");
        }

        DishModel updatedDish = DishModel.builder()
                .id(currentDish.getId())
                .idRestaurante(currentDish.getIdRestaurante())
                .nombre(currentDish.getNombre())
                .precio(dishModel.getPrecio())
                .descripcion(dishModel.getDescripcion())
                .urlImagen(currentDish.getUrlImagen())
                .categoria(currentDish.getCategoria())
                .activo(currentDish.getActivo())
                .build();
        catalogPersistencePort.updateDish(updatedDish);
    }

    @Override
    public void setDishActive(Long idDish, Long ownerId, boolean active) {
        DishModel currentDish = catalogPersistencePort.findDishById(idDish)
                .orElseThrow(() -> notFound("Plato no existe"));
        RestaurantModel restaurant = catalogPersistencePort.findRestaurantById(currentDish.getIdRestaurante())
                .orElseThrow(() -> notFound("Restaurante no existe"));
        if (!restaurant.getIdPropietario().equals(ownerId)) {
            throw forbidden("No puedes modificar platos de otro restaurante");
        }
        DishModel updatedDish = DishModel.builder()
                .id(currentDish.getId())
                .idRestaurante(currentDish.getIdRestaurante())
                .nombre(currentDish.getNombre())
                .precio(currentDish.getPrecio())
                .descripcion(currentDish.getDescripcion())
                .urlImagen(currentDish.getUrlImagen())
                .categoria(currentDish.getCategoria())
                .activo(active)
                .build();
        catalogPersistencePort.updateDish(updatedDish);
    }

    @Override
    public Page<RestaurantModel> listRestaurants(Pageable pageable) {
        return catalogPersistencePort.listRestaurants(pageable);
    }

    @Override
    public Page<DishModel> listDishes(Long restaurantId, String categoria, Pageable pageable) {
        return catalogPersistencePort.listActiveDishes(restaurantId, categoria, pageable);
    }

    private ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseStatusException forbidden(String msg) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
    }

    private ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }
}
