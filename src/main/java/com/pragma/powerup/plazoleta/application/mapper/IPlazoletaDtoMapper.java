package com.pragma.powerup.plazoleta.application.mapper;

import com.pragma.powerup.plazoleta.application.dto.request.CreateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateOrderRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.application.dto.request.UpdateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.response.DishResponse;
import com.pragma.powerup.plazoleta.application.dto.response.EficienciaResponse;
import com.pragma.powerup.plazoleta.application.dto.response.OrderResponse;
import com.pragma.powerup.plazoleta.application.dto.response.RestaurantCardResponse;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyEmployeeModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface IPlazoletaDtoMapper {

    // ── Peticiones a modelos de dominio ───────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    RestaurantModel toRestaurantModel(CreateRestaurantRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    DishModel toDishModel(CreateDishRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idRestaurante", ignore = true)
    @Mapping(target = "nombre", ignore = true)
    @Mapping(target = "urlImagen", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "activo", ignore = true)
    DishModel toDishUpdateModel(UpdateDishRequest request);

    @Mapping(target = "nombrePlato", ignore = true)
    OrderItemModel toOrderItemModel(CreateOrderRequest.OrderDish orderDish);

    List<OrderItemModel> toOrderItemModels(List<CreateOrderRequest.OrderDish> platos);

    default List<OrderItemModel> toOrderItems(CreateOrderRequest request) {
        if (request == null || request.platos() == null) {
            return Collections.emptyList();
        }
        return toOrderItemModels(request.platos());
    }

    // ── Modelos de dominio a respuestas ───────────────────────────────────────

    RestaurantCardResponse toRestaurantCardResponse(RestaurantModel model);

    DishResponse toDishResponse(DishModel model);

    OrderResponse.OrderItemResponse toOrderItemResponse(OrderItemModel item);

    @Mapping(target = "idPedido", source = "id")
    @Mapping(target = "lineas", source = "items")
    @Mapping(target = "estadoActual", expression = "java(model.getEstado().name())")
    OrderResponse toOrderResponse(OrderModel model);

    @Mapping(target = "idEmpleadoAsignado", source = "idEmpleado")
    EficienciaResponse.EficienciaEmpleado toEficienciaEmpleado(OrderEfficiencyEmployeeModel model);

    @Mapping(target = "ranking", source = "ranking")
    EficienciaResponse toEficienciaResponse(OrderEfficiencyModel model);
}
