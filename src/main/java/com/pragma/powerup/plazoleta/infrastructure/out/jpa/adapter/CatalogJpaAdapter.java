package com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.EmployeeRestaurantEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper.IDishEntityMapper;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper.IRestaurantEntityMapper;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.DishRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.RestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

public class CatalogJpaAdapter implements CatalogPersistencePort {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final EmployeeRestaurantRepository employeeRestaurantRepository;
    private final IRestaurantEntityMapper restaurantEntityMapper;
    private final IDishEntityMapper dishEntityMapper;

    public CatalogJpaAdapter(RestaurantRepository restaurantRepository,
                             DishRepository dishRepository,
                             EmployeeRestaurantRepository employeeRestaurantRepository,
                             IRestaurantEntityMapper restaurantEntityMapper,
                             IDishEntityMapper dishEntityMapper) {
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
        this.employeeRestaurantRepository = employeeRestaurantRepository;
        this.restaurantEntityMapper = restaurantEntityMapper;
        this.dishEntityMapper = dishEntityMapper;
    }

    @Override
    public Long createRestaurant(RestaurantModel restaurantModel) {
        RestaurantEntity entity = restaurantEntityMapper.toRestaurantEntity(restaurantModel);
        return restaurantRepository.save(entity).getId();
    }

    @Override
    public Optional<RestaurantModel> findRestaurantById(Long id) {
        return restaurantRepository.findById(id).map(restaurantEntityMapper::toRestaurantModel);
    }

    @Override
    public Long createDish(DishModel dishModel) {
        RestaurantEntity restaurant = restaurantRepository.findById(dishModel.getIdRestaurante())
                .orElseThrow();
        DishEntity entity = dishEntityMapper.toDishEntity(dishModel, restaurant);
        return dishRepository.save(entity).getId();
    }

    @Override
    public Optional<DishModel> findDishById(Long id) {
        return dishRepository.findById(id).map(dishEntityMapper::toDishModel);
    }

    @Override
    public void updateDish(DishModel dishModel) {
        DishEntity entity = dishRepository.findById(dishModel.getId()).orElseThrow();
        dishEntityMapper.updateDishEntity(dishModel, entity);
    }

    @Override
    public PageResult<RestaurantModel> listRestaurants(PaginationParams pagination) {
        Page<RestaurantModel> page = restaurantRepository
                .findAll(PageRequest.of(pagination.page(), pagination.size()))
                .map(restaurantEntityMapper::toRestaurantModel);
        return toPageResult(page);
    }

    @Override
    public PageResult<DishModel> listActiveDishes(Long restaurantId, String categoria, PaginationParams pagination) {
        PageRequest pageRequest = PageRequest.of(pagination.page(), pagination.size());
        Page<DishEntity> page = (categoria == null || categoria.isBlank())
                ? dishRepository.findByRestaurantIdAndActivoTrue(restaurantId, pageRequest)
                : dishRepository.findByRestaurantIdAndActivoTrueAndCategoriaIgnoreCase(restaurantId, categoria, pageRequest);
        return toPageResult(page.map(dishEntityMapper::toDishModel));
    }

    @Override
    public boolean existsEmployeeAssignment(Long employeeId, Long restaurantId) {
        return employeeRestaurantRepository.existsByIdEmpleadoAndRestaurant_Id(employeeId, restaurantId);
    }

    @Override
    public Long saveEmployeeAssignment(Long employeeId, Long restaurantId) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId).orElseThrow();
        EmployeeRestaurantEntity entity = new EmployeeRestaurantEntity();
        entity.setIdEmpleado(employeeId);
        entity.setRestaurant(restaurant);
        return employeeRestaurantRepository.save(entity).getId();
    }

    private <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
