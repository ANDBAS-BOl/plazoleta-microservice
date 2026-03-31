package com.pragma.powerup.plazoleta.service;

import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.*;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.repository.DishRepository;
import com.pragma.powerup.plazoleta.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.repository.OrderRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import com.pragma.powerup.plazoleta.infrastructure.security.AuthUtils;
import com.pragma.powerup.plazoleta.infrastructure.security.Rol;
import com.pragma.powerup.plazoleta.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlazoletaService {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final EmployeeRestaurantRepository employeeRestaurantRepository;
    private final CatalogUseCasePort catalogUseCasePort;
    private final OrderUseCasePort orderUseCasePort;
    private final OrderTraceQueryUseCasePort orderTraceQueryUseCasePort;
    private final OrderEfficiencyUseCasePort orderEfficiencyUseCasePort;
    private final AuthUtils authUtils;
    private final UsuariosClient usuariosClient;

    @Transactional
    public Long createRestaurant(CreateRestaurantRequest request) {
        requireRole(Rol.ADMINISTRADOR);
        RestaurantModel model = RestaurantModel.builder()
                .nombre(request.nombre())
                .nit(request.nit())
                .direccion(request.direccion())
                .telefono(request.telefono())
                .urlLogo(request.urlLogo())
                .idPropietario(request.idPropietario())
                .build();
        return catalogUseCasePort.createRestaurant(model);
    }

    @Transactional
    public Long createDish(CreateDishRequest request) {
        Long userId = requireRole(Rol.PROPIETARIO);
        DishModel model = DishModel.builder()
                .idRestaurante(request.idRestaurante())
                .nombre(request.nombre())
                .precio(request.precio())
                .descripcion(request.descripcion())
                .urlImagen(request.urlImagen())
                .categoria(request.categoria())
                .build();
        return catalogUseCasePort.createDish(model, userId);
    }

    @Transactional
    public void updateDish(Long idDish, UpdateDishRequest request) {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        DishModel model = DishModel.builder()
                .precio(request.precio())
                .descripcion(request.descripcion())
                .build();
        catalogUseCasePort.updateDish(idDish, ownerId, model);
    }

    @Transactional
    public void setDishActive(Long idDish, boolean active) {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        catalogUseCasePort.setDishActive(idDish, ownerId, active);
    }

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

    public Page<RestaurantCardResponse> listRestaurants(Pageable pageable) {
        requireRole(Rol.CLIENTE);
        return catalogUseCasePort.listRestaurants(pageable)
                .map(r -> new RestaurantCardResponse(r.getId(), r.getNombre(), r.getUrlLogo()));
    }

    public Page<DishResponse> listDishes(Long restaurantId, String categoria, Pageable pageable) {
        requireRole(Rol.CLIENTE);
        return catalogUseCasePort.listDishes(restaurantId, categoria, pageable)
                .map(this::toDishResponse);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Long clientId = requireRole(Rol.CLIENTE);
        List<OrderItemModel> items = request.platos() == null ? List.of() : request.platos().stream()
                .map(p -> OrderItemModel.builder()
                        .idPlato(p.idPlato())
                        .cantidad(p.cantidad())
                        .build())
                .collect(Collectors.toList());
        OrderModel created = orderUseCasePort.createOrder(clientId, request.idRestaurante(), request.telefonoCliente(), items);
        return toOrderResponse(created);
    }

    public Page<OrderResponse> listOrdersByStatus(EstadoPedido estado, Pageable pageable) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        return orderUseCasePort.listOrdersByStatus(employeeId, EstadoPedidoModel.valueOf(estado.name()), pageable)
                .map(this::toOrderResponse);
    }

    @Transactional
    public OrderResponse takeOrder(Long idOrder) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        return toOrderResponse(orderUseCasePort.takeOrder(idOrder, employeeId));
    }

    @Transactional
    public OrderResponse markReady(Long idOrder) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        return toOrderResponse(orderUseCasePort.markReady(idOrder, employeeId));
    }

    @Transactional
    public OrderResponse deliverOrder(Long idOrder, DeliverOrderRequest request) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        return toOrderResponse(orderUseCasePort.deliverOrder(idOrder, employeeId, request.pin()));
    }

    @Transactional
    public OrderResponse cancelOrder(Long idOrder) {
        Long clientId = requireRole(Rol.CLIENTE);
        return toOrderResponse(orderUseCasePort.cancelOrder(idOrder, clientId));
    }

    public Object getTrace(Long idOrder) {
        requireRole(Rol.CLIENTE);
        return orderTraceQueryUseCasePort.getTraceByOrderId(idOrder);
    }

    public EficienciaResponse getEfficiency() {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        OrderEfficiencyModel efficiencyModel = orderEfficiencyUseCasePort.getOwnerEfficiency(ownerId);
        return new EficienciaResponse(
                efficiencyModel.ranking().stream()
                        .map(item -> new EficienciaResponse.EficienciaEmpleado(item.idEmpleado(), item.tiempoPromedioMinutos()))
                        .toList()
        );
    }

    private DishResponse toDishResponse(DishModel d) {
        return new DishResponse(d.getId(), d.getNombre(), d.getPrecio(), d.getDescripcion(), d.getUrlImagen(), d.getCategoria(), d.getActivo());
    }

    private OrderResponse toOrderResponse(OrderModel o) {
        List<OrderResponse.OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderResponse.OrderItemResponse(i.getIdPlato(), i.getNombrePlato(), i.getCantidad()))
                .collect(Collectors.toList());
        return new OrderResponse(
                o.getId(),
                o.getIdRestaurante(),
                o.getIdCliente(),
                items,
                o.getEstado().name(),
                o.getFechaCreacion(),
                o.getIdEmpleadoAsignado());
    }

    private Long requireRole(Rol requiredRole) {
        var user = authUtils.currentUser();
        if (user.getRol() != requiredRole) {
            throw forbidden("No tienes permisos para esta operacion");
        }
        return user.getId();
    }

    private ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseStatusException forbidden(String msg) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
    }

    private ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }
}
