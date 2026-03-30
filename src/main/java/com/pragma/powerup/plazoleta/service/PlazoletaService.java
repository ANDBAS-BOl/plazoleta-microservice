package com.pragma.powerup.plazoleta.service;

import com.pragma.powerup.plazoleta.client.AuthHeaderProvider;
import com.pragma.powerup.plazoleta.client.MensajeriaClient;
import com.pragma.powerup.plazoleta.client.TrazabilidadClient;
import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.*;
import com.pragma.powerup.plazoleta.domain.model.DishModel;
import com.pragma.powerup.plazoleta.domain.model.RestaurantModel;
import com.pragma.powerup.plazoleta.repository.DishRepository;
import com.pragma.powerup.plazoleta.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.repository.OrderRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import com.pragma.powerup.plazoleta.security.AuthUtils;
import com.pragma.powerup.plazoleta.security.Rol;
import com.pragma.powerup.plazoleta.service.pin.PinGenerator;
import com.pragma.powerup.plazoleta.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
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
    private final AuthUtils authUtils;
    private final UsuariosClient usuariosClient;
    private final TrazabilidadClient trazabilidadClient;
    private final MensajeriaClient mensajeriaClient;
    private final PinGenerator pinGenerator;
    private final AuthHeaderProvider authHeaderProvider;

    private static final int MAX_PIN_GENERATION_ATTEMPTS = 10;

    @Value("${microservices.trazabilidad.url}")
    private String trazabilidadBaseUrl;

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
        if (orderRepository.existsByIdClienteAndEstadoIn(
                clientId,
                List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO))) {
            throw badRequest("Ya tienes un pedido en proceso");
        }
        if (request.platos() == null || request.platos().isEmpty()) {
            throw badRequest("El pedido debe contener platos");
        }
        RestaurantEntity restaurant = restaurantRepository.findById(request.idRestaurante())
                .orElseThrow(() -> notFound("Restaurante no existe"));

        OrderEntity order = new OrderEntity();
        order.setRestaurant(restaurant);
        order.setIdCliente(clientId);
        order.setTelefonoCliente(request.telefonoCliente() == null ? "0000000000" : request.telefonoCliente());
        order.setFechaCreacion(LocalDateTime.now());
        order.setEstado(EstadoPedido.PENDIENTE);

        for (CreateOrderRequest.OrderDish dishReq : request.platos()) {
            DishEntity dish = dishRepository.findById(dishReq.idPlato()).orElseThrow(() -> notFound("Plato no existe"));
            if (!dish.getActivo()) {
                throw badRequest("No se pueden pedir platos inactivos");
            }
            if (!dish.getRestaurant().getId().equals(request.idRestaurante())) {
                throw badRequest("Un pedido debe incluir platos de un solo restaurante");
            }
            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setDish(dish);
            item.setCantidad(dishReq.cantidad());
            order.getItems().add(item);
        }

        OrderEntity saved = orderRepository.save(order);
        registerTrace(saved, null, EstadoPedido.PENDIENTE);
        return toOrderResponse(saved);
    }

    public Page<OrderResponse> listOrdersByStatus(EstadoPedido estado, Pageable pageable) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        EmployeeRestaurantEntity employeeRestaurant = employeeRestaurantRepository.findFirstByIdEmpleado(employeeId)
                .orElseThrow(() -> forbidden("El empleado no esta asignado a un restaurante"));
        return orderRepository.findByRestaurantIdAndEstado(employeeRestaurant.getRestaurant().getId(), estado, pageable)
                .map(this::toOrderResponse);
    }

    @Transactional
    public OrderResponse takeOrder(Long idOrder) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        OrderEntity order = orderRepository.findById(idOrder).orElseThrow(() -> notFound("Pedido no existe"));
        EmployeeRestaurantEntity employeeRestaurant = employeeRestaurantRepository.findFirstByIdEmpleado(employeeId)
                .orElseThrow(() -> forbidden("El empleado no esta asignado a un restaurante"));
        if (!employeeRestaurant.getRestaurant().getId().equals(order.getRestaurant().getId())) {
            throw forbidden("Solo puedes tomar pedidos de tu restaurante");
        }
        int updated = orderRepository.takeOrderIfPending(idOrder, EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION, employeeId);
        if (updated == 0) {
            throw badRequest("El pedido ya fue asignado o no esta pendiente");
        }
        OrderEntity current = orderRepository.findById(idOrder).orElseThrow(() -> notFound("Pedido no existe"));
        registerTrace(current, EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION);
        return toOrderResponse(current);
    }

    @Transactional
    public OrderResponse markReady(Long idOrder) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        OrderEntity order = orderRepository.findById(idOrder).orElseThrow(() -> notFound("Pedido no existe"));
        if (!employeeId.equals(order.getIdEmpleadoAsignado())) {
            throw forbidden("Solo el empleado asignado puede marcar LISTO");
        }
        if (order.getEstado() != EstadoPedido.EN_PREPARACION) {
            throw badRequest("Solo pedidos EN_PREPARACION pueden pasar a LISTO");
        }
        // Regla HU 14: si Mensajería falla, el pedido NO debe quedar en LISTO.
        String pin = generateUniquePin();
        mensajeriaClient.enviarSms(order.getTelefonoCliente(), "Tu pedido esta listo. PIN: " + pin);

        order.setPinSeguridad(pin);
        order.setEstado(EstadoPedido.LISTO);
        registerTrace(order, EstadoPedido.EN_PREPARACION, EstadoPedido.LISTO);
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse deliverOrder(Long idOrder, DeliverOrderRequest request) {
        Long employeeId = requireRole(Rol.EMPLEADO);
        OrderEntity order = orderRepository.findById(idOrder).orElseThrow(() -> notFound("Pedido no existe"));
        if (!employeeId.equals(order.getIdEmpleadoAsignado())) {
            throw forbidden("Solo el empleado asignado puede entregar");
        }
        if (order.getEstado() != EstadoPedido.LISTO) {
            throw badRequest("Solo pedidos LISTO pueden pasar a ENTREGADO");
        }
        if (!Objects.equals(order.getPinSeguridad(), request.pin())) {
            throw badRequest("PIN invalido");
        }
        order.setEstado(EstadoPedido.ENTREGADO);
        order.setFechaEntrega(LocalDateTime.now());
        order.setPinSeguridad(null);
        registerTrace(order, EstadoPedido.LISTO, EstadoPedido.ENTREGADO);
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long idOrder) {
        Long clientId = requireRole(Rol.CLIENTE);
        OrderEntity order = orderRepository.findById(idOrder).orElseThrow(() -> notFound("Pedido no existe"));
        if (!order.getIdCliente().equals(clientId)) {
            throw forbidden("No puedes cancelar pedidos de otro cliente");
        }
        if (order.getEstado() != EstadoPedido.PENDIENTE) {
            throw badRequest("Lo sentimos, tu pedido ya esta en preparacion y no puede cancelarse");
        }
        order.setEstado(EstadoPedido.CANCELADO);
        registerTrace(order, EstadoPedido.PENDIENTE, EstadoPedido.CANCELADO);
        return toOrderResponse(order);
    }

    public Object getTrace(Long idOrder) {
        Long clientId = requireRole(Rol.CLIENTE);
        String url = trazabilidadBaseUrl + "/api/v1/trazabilidad/pedidos/" + idOrder;
        WebClient.RequestHeadersSpec<?> spec = WebClient.create(url).get();
        authHeaderProvider.getAuthorizationHeader()
                .ifPresent(token -> spec.header(HttpHeaders.AUTHORIZATION, token));

        return spec
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public EficienciaResponse getEfficiency() {
        Long ownerId = requireRole(Rol.PROPIETARIO);
        List<Long> restaurantIds = restaurantRepository.findAll().stream()
                .filter(r -> r.getIdPropietario().equals(ownerId))
                .map(RestaurantEntity::getId)
                .collect(Collectors.toList());
        if (restaurantIds.isEmpty()) {
            return new EficienciaResponse(List.of());
        }
        List<OrderEntity> delivered = orderRepository.findByRestaurantIdInAndEstado(restaurantIds, EstadoPedido.ENTREGADO);
        Map<Long, List<Long>> minutesByEmployee = new HashMap<>();
        for (OrderEntity order : delivered) {
            if (order.getIdEmpleadoAsignado() == null || order.getFechaEntrega() == null) {
                continue;
            }
            long minutes = Duration.between(order.getFechaCreacion(), order.getFechaEntrega()).toMinutes();
            minutesByEmployee.computeIfAbsent(order.getIdEmpleadoAsignado(), key -> new ArrayList<>()).add(minutes);
        }
        List<EficienciaResponse.EficienciaEmpleado> ranking = minutesByEmployee.entrySet().stream()
                .map(e -> new EficienciaResponse.EficienciaEmpleado(
                        e.getKey(),
                        e.getValue().stream().mapToLong(v -> v).average().orElse(0)))
                .sorted(Comparator.comparing(EficienciaResponse.EficienciaEmpleado::tiempoPromedioMinutos))
                .collect(Collectors.toList());
        return new EficienciaResponse(ranking);
    }

    private void registerTrace(OrderEntity order, EstadoPedido from, EstadoPedido to) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("idPedido", order.getId());
        payload.put("idCliente", order.getIdCliente());
        payload.put("idRestaurante", order.getRestaurant().getId());
        payload.put("idEmpleado", order.getIdEmpleadoAsignado());
        payload.put("estadoAnterior", from == null ? null : from.name());
        payload.put("estadoNuevo", to.name());
        try {
            trazabilidadClient.registrarEvento(payload);
        } catch (Exception ignored) {
            // No se bloquea el flujo por un error externo de trazabilidad.
        }
    }

    private String generateUniquePin() {
        for (int attempt = 0; attempt < MAX_PIN_GENERATION_ATTEMPTS; attempt++) {
            String pin = pinGenerator.generarPin6Digitos();
            // Unicidad global del PIN mientras está activo (cuando se entrega se invalida a null).
            if (!orderRepository.existsByPinSeguridad(pin)) {
                return pin;
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar un PIN único");
    }

    private DishResponse toDishResponse(DishModel d) {
        return new DishResponse(d.getId(), d.getNombre(), d.getPrecio(), d.getDescripcion(), d.getUrlImagen(), d.getCategoria(), d.getActivo());
    }

    private OrderResponse toOrderResponse(OrderEntity o) {
        List<OrderResponse.OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderResponse.OrderItemResponse(i.getDish().getId(), i.getDish().getNombre(), i.getCantidad()))
                .collect(Collectors.toList());
        return new OrderResponse(
                o.getId(),
                o.getRestaurant().getId(),
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
