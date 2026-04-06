package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.exception.AccessDeniedException;
import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.exception.ResourceNotFoundException;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;

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
            throw new BusinessRuleException("El NIT debe ser numerico");
        }
        if (!restaurantModel.getTelefono().matches("^\\+?\\d{1,13}$")) {
            throw new BusinessRuleException("Telefono invalido");
        }
        if (restaurantModel.getNombre().matches("^\\d+$")) {
            throw new BusinessRuleException("Nombre de restaurante invalido");
        }
        if (!usuariosValidationPort.isPropietario(restaurantModel.getIdPropietario())) {
            throw new BusinessRuleException("El idPropietario no corresponde a un usuario con rol PROPIETARIO");
        }

        return catalogPersistencePort.createRestaurant(restaurantModel);
    }

    @Override
    public Long createDish(DishModel dishModel, Long ownerId) {
        RestaurantModel restaurant = catalogPersistencePort.findRestaurantById(dishModel.getIdRestaurante())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante no existe"));

        if (!restaurant.getIdPropietario().equals(ownerId)) {
            throw new AccessDeniedException("No puedes crear platos en restaurantes de otro propietario");
        }
        if (dishModel.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El precio debe ser mayor a 0");
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
                .orElseThrow(() -> new ResourceNotFoundException("Plato no existe"));

        RestaurantModel restaurant = catalogPersistencePort.findRestaurantById(currentDish.getIdRestaurante())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante no existe"));

        if (!restaurant.getIdPropietario().equals(ownerId)) {
            throw new AccessDeniedException("No puedes modificar platos de otro restaurante");
        }
        if (dishModel.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El precio debe ser mayor a 0");
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
                .orElseThrow(() -> new ResourceNotFoundException("Plato no existe"));
        RestaurantModel restaurant = catalogPersistencePort.findRestaurantById(currentDish.getIdRestaurante())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante no existe"));
        if (!restaurant.getIdPropietario().equals(ownerId)) {
            throw new AccessDeniedException("No puedes modificar platos de otro restaurante");
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
    public PageResult<RestaurantModel> listRestaurants(PaginationParams pagination) {
        return catalogPersistencePort.listRestaurants(pagination);
    }

    @Override
    public PageResult<DishModel> listDishes(Long restaurantId, String categoria, PaginationParams pagination) {
        return catalogPersistencePort.listActiveDishes(restaurantId, categoria, pagination);
    }

    @Override
    public Long assignEmployeeToRestaurant(Long restaurantId, Long employeeId, Long ownerId) {
        RestaurantModel restaurant = catalogPersistencePort.findRestaurantById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante no existe"));

        if (!restaurant.getIdPropietario().equals(ownerId)) {
            throw new AccessDeniedException("No puedes asignar empleados en restaurantes de otro propietario");
        }
        if (!usuariosValidationPort.isEmpleado(employeeId)) {
            throw new BusinessRuleException("El idEmpleado no corresponde a un usuario con rol EMPLEADO");
        }
        if (catalogPersistencePort.existsEmployeeAssignment(employeeId, restaurantId)) {
            throw new BusinessRuleException("El empleado ya esta asignado a este restaurante");
        }

        return catalogPersistencePort.saveEmployeeAssignment(employeeId, restaurantId);
    }
}
