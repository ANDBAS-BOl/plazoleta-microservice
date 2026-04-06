package com.pragma.powerup.plazoleta.application.handler.impl;

import com.pragma.powerup.plazoleta.application.dto.request.AssignEmployeeRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.application.dto.request.UpdateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.response.DishResponse;
import com.pragma.powerup.plazoleta.application.dto.response.RestaurantCardResponse;
import com.pragma.powerup.plazoleta.application.handler.ICatalogHandler;
import com.pragma.powerup.plazoleta.application.mapper.PlazoletaDtoMapper;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CatalogHandler implements ICatalogHandler {

    private final CatalogUseCasePort catalogUseCasePort;

    @Override
    @Transactional
    public Long createRestaurant(CreateRestaurantRequest request) {
        return catalogUseCasePort.createRestaurant(PlazoletaDtoMapper.toRestaurantModel(request));
    }

    @Override
    @Transactional
    public Long createDish(CreateDishRequest request, Long ownerId) {
        return catalogUseCasePort.createDish(PlazoletaDtoMapper.toDishModel(request), ownerId);
    }

    @Override
    @Transactional
    public Long assignEmployeeToRestaurant(Long idRestaurante, AssignEmployeeRequest request, Long ownerId) {
        return catalogUseCasePort.assignEmployeeToRestaurant(idRestaurante, request.idEmpleado(), ownerId);
    }

    @Override
    @Transactional
    public void updateDish(Long idPlato, UpdateDishRequest request, Long ownerId) {
        catalogUseCasePort.updateDish(idPlato, ownerId, PlazoletaDtoMapper.toDishUpdateModel(request));
    }

    @Override
    @Transactional
    public void changeDishStatus(Long idPlato, boolean activo, Long ownerId) {
        catalogUseCasePort.setDishActive(idPlato, ownerId, activo);
    }

    @Override
    public PageResult<RestaurantCardResponse> listRestaurants(int page, int size) {
        return catalogUseCasePort
                .listRestaurants(new PaginationParams(page, size))
                .map(PlazoletaDtoMapper::toRestaurantCardResponse);
    }

    @Override
    public PageResult<DishResponse> listDishes(Long idRestaurante, String categoria, int page, int size) {
        return catalogUseCasePort
                .listDishes(idRestaurante, categoria, new PaginationParams(page, size))
                .map(PlazoletaDtoMapper::toDishResponse);
    }
}
