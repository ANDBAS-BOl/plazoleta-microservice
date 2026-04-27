package com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper;

import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IRestaurantEntityMapper {

    RestaurantEntity toRestaurantEntity(RestaurantModel model);

    RestaurantModel toRestaurantModel(RestaurantEntity entity);
}
