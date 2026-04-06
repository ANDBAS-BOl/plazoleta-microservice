package com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.EmployeeRestaurantEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.DishRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.RestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.stream.Collectors;

public class CatalogJpaAdapter implements CatalogPersistencePort {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final EmployeeRestaurantRepository employeeRestaurantRepository;

    public CatalogJpaAdapter(RestaurantRepository restaurantRepository,
                             DishRepository dishRepository,
                             EmployeeRestaurantRepository employeeRestaurantRepository) {
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
        this.employeeRestaurantRepository = employeeRestaurantRepository;
    }

    @Override
    public Long createRestaurant(RestaurantModel restaurantModel) {
        RestaurantEntity entity = new RestaurantEntity();
        entity.setNombre(restaurantModel.getNombre());
        entity.setNit(restaurantModel.getNit());
        entity.setDireccion(restaurantModel.getDireccion());
        entity.setTelefono(restaurantModel.getTelefono());
        entity.setUrlLogo(restaurantModel.getUrlLogo());
        entity.setIdPropietario(restaurantModel.getIdPropietario());
        return restaurantRepository.save(entity).getId();
    }

    @Override
    public Optional<RestaurantModel> findRestaurantById(Long id) {
        return restaurantRepository.findById(id).map(this::toRestaurantModel);
    }

    @Override
    public Long createDish(DishModel dishModel) {
        RestaurantEntity restaurant = restaurantRepository.findById(dishModel.getIdRestaurante())
                .orElseThrow();
        DishEntity entity = new DishEntity();
        entity.setNombre(dishModel.getNombre());
        entity.setPrecio(dishModel.getPrecio());
        entity.setDescripcion(dishModel.getDescripcion());
        entity.setUrlImagen(dishModel.getUrlImagen());
        entity.setCategoria(dishModel.getCategoria());
        entity.setActivo(Boolean.TRUE.equals(dishModel.getActivo()));
        entity.setRestaurant(restaurant);
        return dishRepository.save(entity).getId();
    }

    @Override
    public Optional<DishModel> findDishById(Long id) {
        return dishRepository.findById(id).map(this::toDishModel);
    }

    @Override
    public void updateDish(DishModel dishModel) {
        DishEntity entity = dishRepository.findById(dishModel.getId()).orElseThrow();
        entity.setPrecio(dishModel.getPrecio());
        entity.setDescripcion(dishModel.getDescripcion());
        entity.setActivo(Boolean.TRUE.equals(dishModel.getActivo()));
    }

    @Override
    public PageResult<RestaurantModel> listRestaurants(PaginationParams pagination) {
        Page<RestaurantEntity> page = restaurantRepository.findAll(PageRequest.of(pagination.page(), pagination.size()));
        return toPageResult(page.map(this::toRestaurantModel));
    }

    @Override
    public PageResult<DishModel> listActiveDishes(Long restaurantId, String categoria, PaginationParams pagination) {
        PageRequest pageRequest = PageRequest.of(pagination.page(), pagination.size());
        Page<DishEntity> page = (categoria == null || categoria.isBlank())
                ? dishRepository.findByRestaurantIdAndActivoTrue(restaurantId, pageRequest)
                : dishRepository.findByRestaurantIdAndActivoTrueAndCategoriaIgnoreCase(restaurantId, categoria, pageRequest);
        return toPageResult(page.map(this::toDishModel));
    }

    private <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private RestaurantModel toRestaurantModel(RestaurantEntity entity) {
        return RestaurantModel.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .nit(entity.getNit())
                .direccion(entity.getDireccion())
                .telefono(entity.getTelefono())
                .urlLogo(entity.getUrlLogo())
                .idPropietario(entity.getIdPropietario())
                .build();
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

    private DishModel toDishModel(DishEntity entity) {
        return DishModel.builder()
                .id(entity.getId())
                .idRestaurante(entity.getRestaurant().getId())
                .nombre(entity.getNombre())
                .precio(entity.getPrecio())
                .descripcion(entity.getDescripcion())
                .urlImagen(entity.getUrlImagen())
                .categoria(entity.getCategoria())
                .activo(entity.getActivo())
                .build();
    }
}
