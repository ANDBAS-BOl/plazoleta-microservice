package com.pragma.powerup.plazoleta.application.mapper;

import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.web.dto.CreateDishRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateOrderRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.web.dto.DishResponse;
import com.pragma.powerup.plazoleta.web.dto.EficienciaResponse;
import com.pragma.powerup.plazoleta.web.dto.OrderResponse;
import com.pragma.powerup.plazoleta.web.dto.RestaurantCardResponse;
import com.pragma.powerup.plazoleta.web.dto.UpdateDishRequest;

import java.util.List;
import java.util.stream.Collectors;

public final class PlazoletaDtoMapper {

    private PlazoletaDtoMapper() {
    }

    public static RestaurantModel toRestaurantModel(CreateRestaurantRequest request) {
        return RestaurantModel.builder()
                .nombre(request.nombre())
                .nit(request.nit())
                .direccion(request.direccion())
                .telefono(request.telefono())
                .urlLogo(request.urlLogo())
                .idPropietario(request.idPropietario())
                .build();
    }

    public static DishModel toDishModel(CreateDishRequest request) {
        return DishModel.builder()
                .idRestaurante(request.idRestaurante())
                .nombre(request.nombre())
                .precio(request.precio())
                .descripcion(request.descripcion())
                .urlImagen(request.urlImagen())
                .categoria(request.categoria())
                .build();
    }

    public static DishModel toDishUpdateModel(UpdateDishRequest request) {
        return DishModel.builder()
                .precio(request.precio())
                .descripcion(request.descripcion())
                .build();
    }

    public static List<OrderItemModel> toOrderItems(CreateOrderRequest request) {
        if (request.platos() == null) {
            return List.of();
        }
        return request.platos().stream()
                .map(item -> OrderItemModel.builder()
                        .idPlato(item.idPlato())
                        .cantidad(item.cantidad())
                        .build())
                .collect(Collectors.toList());
    }

    public static RestaurantCardResponse toRestaurantCardResponse(RestaurantModel model) {
        return new RestaurantCardResponse(model.getId(), model.getNombre(), model.getUrlLogo());
    }

    public static DishResponse toDishResponse(DishModel model) {
        return new DishResponse(
                model.getId(),
                model.getNombre(),
                model.getPrecio(),
                model.getDescripcion(),
                model.getUrlImagen(),
                model.getCategoria(),
                model.getActivo());
    }

    public static OrderResponse toOrderResponse(OrderModel model) {
        List<OrderResponse.OrderItemResponse> items = model.getItems().stream()
                .map(item -> new OrderResponse.OrderItemResponse(item.getIdPlato(), item.getNombrePlato(), item.getCantidad()))
                .collect(Collectors.toList());
        return new OrderResponse(
                model.getId(),
                model.getIdRestaurante(),
                model.getIdCliente(),
                items,
                model.getEstado().name(),
                model.getFechaCreacion(),
                model.getIdEmpleadoAsignado());
    }

    public static EficienciaResponse toEfficiencyResponse(OrderEfficiencyModel model) {
        return new EficienciaResponse(
                model.ranking().stream()
                        .map(item -> new EficienciaResponse.EficienciaEmpleado(item.idEmpleado(), item.tiempoPromedioMinutos()))
                        .toList()
        );
    }
}
