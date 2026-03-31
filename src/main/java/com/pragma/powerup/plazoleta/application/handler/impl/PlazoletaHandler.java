package com.pragma.powerup.plazoleta.application.handler.impl;

import com.pragma.powerup.plazoleta.application.handler.IPlazoletaHandler;
import com.pragma.powerup.plazoleta.application.mapper.PlazoletaDtoMapper;
import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.domain.EmployeeRestaurantEntity;
import com.pragma.powerup.plazoleta.domain.EstadoPedido;
import com.pragma.powerup.plazoleta.domain.RestaurantEntity;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import com.pragma.powerup.plazoleta.infrastructure.security.AuthUtils;
import com.pragma.powerup.plazoleta.infrastructure.security.Rol;
import com.pragma.powerup.plazoleta.web.dto.AssignEmployeeRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateDishRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateOrderRequest;
import com.pragma.powerup.plazoleta.web.dto.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.web.dto.DeliverOrderRequest;
import com.pragma.powerup.plazoleta.web.dto.DishResponse;
import com.pragma.powerup.plazoleta.web.dto.EficienciaResponse;
import com.pragma.powerup.plazoleta.web.dto.OrderResponse;
import com.pragma.powerup.plazoleta.web.dto.RestaurantCardResponse;
import com.pragma.powerup.plazoleta.web.dto.UpdateDishRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class PlazoletaHandler implements IPlazoletaHandler {

    private final CatalogUseCasePort catalogUseCasePort;
    private final OrderUseCasePort orderUseCasePort;
    private final OrderTraceQueryUseCasePort orderTraceQueryUseCasePort;
    private final OrderEfficiencyUseCasePort orderEfficiencyUseCasePort;
    private final RestaurantRepository restaurantRepository;
    private final EmployeeRestaurantRepository employeeRestaurantRepository;
    private final UsuariosClient usuariosClient;
    private final AuthUtils authUtils;

    @Override
    @Transactional
    public Long createRestaurant(CreateRestaurantRequest request) {
        requireRole(Rol.ADMINISTRADOR);
        return catalogUseCasePort.createRestaurant(PlazoletaDtoMapper.toRestaurantModel(request));
    }

    @Override
    @Transactional
    public Long createDish(CreateDishRequest request) {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        return catalogUseCasePort.createDish(PlazoletaDtoMapper.toDishModel(request), ownerId);
    }

    @Override
    @Transactional
    public Long assignEmployeeToRestaurant(Long idRestaurante, AssignEmployeeRequest request) {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        RestaurantEntity restaurant = restaurantRepository.findById(idRestaurante)
                .orElseThrow(() -> notFound("Restaurante no existe"));

        if (!restaurant.getIdPropietario().equals(ownerId)) {
            throw forbidden("No puedes asignar empleados en restaurantes de otro propietario");
        }
        if (!usuariosClient.validarRolEmpleado(request.idEmpleado())) {
            throw badRequest("El idEmpleado no corresponde a un usuario con rol EMPLEADO");
        }
        if (employeeRestaurantRepository.existsByIdEmpleadoAndRestaurant_Id(request.idEmpleado(), idRestaurante)) {
            throw badRequest("El empleado ya esta asignado a este restaurante");
        }

        EmployeeRestaurantEntity entity = new EmployeeRestaurantEntity();
        entity.setIdEmpleado(request.idEmpleado());
        entity.setRestaurant(restaurant);
        return employeeRestaurantRepository.save(entity).getId();
    }

    @Override
    @Transactional
    public void updateDish(Long idPlato, UpdateDishRequest request) {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        catalogUseCasePort.updateDish(idPlato, ownerId, PlazoletaDtoMapper.toDishUpdateModel(request));
    }

    @Override
    @Transactional
    public void changeDishStatus(Long idPlato, boolean activo) {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        catalogUseCasePort.setDishActive(idPlato, ownerId, activo);
    }

    @Override
    public Page<RestaurantCardResponse> listRestaurants(Pageable pageable) {
        requireRole(Rol.CLIENTE);
        return catalogUseCasePort.listRestaurants(pageable).map(PlazoletaDtoMapper::toRestaurantCardResponse);
    }

    @Override
    public Page<DishResponse> listDishes(Long idRestaurante, String categoria, Pageable pageable) {
        requireRole(Rol.CLIENTE);
        return catalogUseCasePort.listDishes(idRestaurante, categoria, pageable).map(PlazoletaDtoMapper::toDishResponse);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Long clientId = requireRole(Rol.CLIENTE);
        OrderModel model = orderUseCasePort.createOrder(
                clientId,
                request.idRestaurante(),
                request.telefonoCliente(),
                PlazoletaDtoMapper.toOrderItems(request));
        return PlazoletaDtoMapper.toOrderResponse(model);
    }

    @Override
    public Page<OrderResponse> listOrdersByStatus(EstadoPedido estado, Pageable pageable) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        return orderUseCasePort
                .listOrdersByStatus(employeeId, EstadoPedidoModel.valueOf(estado.name()), pageable)
                .map(PlazoletaDtoMapper::toOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse takeOrder(Long idPedido) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        return PlazoletaDtoMapper.toOrderResponse(orderUseCasePort.takeOrder(idPedido, employeeId));
    }

    @Override
    @Transactional
    public OrderResponse markReady(Long idPedido) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        return PlazoletaDtoMapper.toOrderResponse(orderUseCasePort.markReady(idPedido, employeeId));
    }

    @Override
    @Transactional
    public OrderResponse deliverOrder(Long idPedido, DeliverOrderRequest request) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        return PlazoletaDtoMapper.toOrderResponse(orderUseCasePort.deliverOrder(idPedido, employeeId, request.pin()));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long idPedido) {
        Long clientId = requireRole(Rol.CLIENTE);
        return PlazoletaDtoMapper.toOrderResponse(orderUseCasePort.cancelOrder(idPedido, clientId));
    }

    @Override
    public Object trace(Long idPedido) {
        requireRole(Rol.CLIENTE);
        return orderTraceQueryUseCasePort.getTraceByOrderId(idPedido);
    }

    @Override
    public EficienciaResponse efficiency() {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        OrderEfficiencyModel model = orderEfficiencyUseCasePort.getOwnerEfficiency(ownerId);
        return PlazoletaDtoMapper.toEfficiencyResponse(model);
    }

    private Long requireRole(Rol requiredRole) {
        var user = authUtils.currentUser();
        if (user.getRol() != requiredRole) {
            throw forbidden("No tienes permisos para esta operacion");
        }
        return user.getId();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
