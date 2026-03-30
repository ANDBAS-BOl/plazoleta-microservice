package com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter;

import com.pragma.powerup.plazoleta.domain.DishEntity;
import com.pragma.powerup.plazoleta.domain.RestaurantEntity;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.repository.DishRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public class CatalogJpaAdapter implements CatalogPersistencePort {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;

    public CatalogJpaAdapter(RestaurantRepository restaurantRepository, DishRepository dishRepository) {
        this.restaurantRepository = restaurantRepository;
        this.dishRepository = dishRepository;
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
    public Page<RestaurantModel> listRestaurants(Pageable pageable) {
        return restaurantRepository.findAll(pageable).map(this::toRestaurantModel);
    }

    @Override
    public Page<DishModel> listActiveDishes(Long restaurantId, String categoria, Pageable pageable) {
        Page<DishEntity> page = (categoria == null || categoria.isBlank())
                ? dishRepository.findByRestaurantIdAndActivoTrue(restaurantId, pageable)
                : dishRepository.findByRestaurantIdAndActivoTrueAndCategoriaIgnoreCase(restaurantId, categoria, pageable);
        return page.map(this::toDishModel);
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
