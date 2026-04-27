package com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.DishEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface IDishEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "restaurant", source = "restaurant")
    @Mapping(target = "nombre", source = "dish.nombre")
    @Mapping(target = "precio", source = "dish.precio")
    @Mapping(target = "descripcion", source = "dish.descripcion")
    @Mapping(target = "urlImagen", source = "dish.urlImagen")
    @Mapping(target = "categoria", source = "dish.categoria")
    @Mapping(target = "activo", source = "dish.activo")
    DishEntity toDishEntity(DishModel dish, RestaurantEntity restaurant);

    @Mapping(target = "idRestaurante", source = "restaurant.id")
    DishModel toDishModel(DishEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nombre", ignore = true)
    @Mapping(target = "urlImagen", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateDishEntity(DishModel source, @MappingTarget DishEntity target);
}
