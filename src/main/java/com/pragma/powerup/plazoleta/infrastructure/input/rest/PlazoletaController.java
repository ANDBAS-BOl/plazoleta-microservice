package com.pragma.powerup.plazoleta.infrastructure.input.rest;

import com.pragma.powerup.plazoleta.application.handler.IPlazoletaHandler;
import com.pragma.powerup.plazoleta.domain.EstadoPedido;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/plazoleta")
@RequiredArgsConstructor
public class PlazoletaController {

    private final IPlazoletaHandler plazoletaHandler;

    @PostMapping("/restaurantes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createRestaurant(@RequestBody CreateRestaurantRequest request) {
        return Map.of("id", plazoletaHandler.createRestaurant(request));
    }

    @PostMapping("/platos")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createDish(@RequestBody CreateDishRequest request) {
        return Map.of("id", plazoletaHandler.createDish(request));
    }

    @PostMapping("/restaurantes/{idRestaurante}/empleados")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> assignEmployeeToRestaurant(@PathVariable Long idRestaurante,
                                                         @RequestBody AssignEmployeeRequest request) {
        return Map.of("id", plazoletaHandler.assignEmployeeToRestaurant(idRestaurante, request));
    }

    @PatchMapping("/platos/{idPlato}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDish(@PathVariable Long idPlato, @RequestBody UpdateDishRequest request) {
        plazoletaHandler.updateDish(idPlato, request);
    }

    @PatchMapping("/platos/{idPlato}/estado")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDishStatus(@PathVariable Long idPlato, @RequestParam boolean activo) {
        plazoletaHandler.changeDishStatus(idPlato, activo);
    }

    @GetMapping("/restaurantes")
    @PreAuthorize("hasRole('CLIENTE')")
    public Page<RestaurantCardResponse> listRestaurants(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return plazoletaHandler.listRestaurants(PageRequest.of(page, size));
    }

    @GetMapping("/restaurantes/{idRestaurante}/platos")
    @PreAuthorize("hasRole('CLIENTE')")
    public Page<DishResponse> listDishes(@PathVariable Long idRestaurante,
                                         @RequestParam(required = false) String categoria,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return plazoletaHandler.listDishes(idRestaurante, categoria, PageRequest.of(page, size));
    }

    @PostMapping("/pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        return plazoletaHandler.createOrder(request);
    }

    @GetMapping("/pedidos")
    @PreAuthorize("hasRole('EMPLEADO')")
    public Page<OrderResponse> listOrdersByStatus(@RequestParam EstadoPedido estado,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return plazoletaHandler.listOrdersByStatus(estado, PageRequest.of(page, size));
    }

    @PatchMapping("/pedidos/{idPedido}/asignar")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse takeOrder(@PathVariable Long idPedido) {
        return plazoletaHandler.takeOrder(idPedido);
    }

    @PatchMapping("/pedidos/{idPedido}/listo")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse markReady(@PathVariable Long idPedido) {
        return plazoletaHandler.markReady(idPedido);
    }

    @PatchMapping("/pedidos/{idPedido}/entregar")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse deliverOrder(@PathVariable Long idPedido, @RequestBody DeliverOrderRequest request) {
        return plazoletaHandler.deliverOrder(idPedido, request);
    }

    @PatchMapping("/pedidos/{idPedido}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public OrderResponse cancelOrder(@PathVariable Long idPedido) {
        return plazoletaHandler.cancelOrder(idPedido);
    }

    @GetMapping("/pedidos/{idPedido}/trazabilidad")
    @PreAuthorize("hasRole('CLIENTE')")
    public Object trace(@PathVariable Long idPedido) {
        return plazoletaHandler.trace(idPedido);
    }

    @GetMapping("/pedidos/eficiencia")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public EficienciaResponse efficiency() {
        return plazoletaHandler.efficiency();
    }
}
