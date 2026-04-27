package com.pragma.powerup.plazoleta.infrastructure.out.jpa.mapper;

import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IOrderEntityMapper {

    @Mapping(target = "idPlato",     source = "dish.id")
    @Mapping(target = "nombrePlato", source = "dish.nombre")
    OrderItemModel toOrderItemModel(OrderItemEntity item);

    @Mapping(target = "idRestaurante", source = "restaurant.id")
    OrderModel toOrderModel(OrderEntity entity);
}
