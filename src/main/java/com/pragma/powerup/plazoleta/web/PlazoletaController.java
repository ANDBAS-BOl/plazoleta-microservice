package com.pragma.powerup.plazoleta.web;

import com.pragma.powerup.plazoleta.domain.EstadoPedido;
import com.pragma.powerup.plazoleta.service.PlazoletaService;
import com.pragma.powerup.plazoleta.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/plazoleta")
@RequiredArgsConstructor
public class PlazoletaController {

    private final PlazoletaService plazoletaService;

    @PostMapping("/restaurantes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createRestaurant(@RequestBody CreateRestaurantRequest request) {
        return Map.of("id", plazoletaService.createRestaurant(request));
    }

    @PostMapping("/platos")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createDish(@RequestBody CreateDishRequest request) {
        return Map.of("id", plazoletaService.createDish(request));
    }

    @PostMapping("/restaurantes/{idRestaurante}/empleados")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> assignEmployeeToRestaurant(@PathVariable Long idRestaurante,
                                                           @RequestBody AssignEmployeeRequest request) {
        return Map.of("id", plazoletaService.assignEmployeeToRestaurant(idRestaurante, request));
    }

    @PatchMapping("/platos/{idPlato}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDish(@PathVariable Long idPlato, @RequestBody UpdateDishRequest request) {
        plazoletaService.updateDish(idPlato, request);
    }

    @PatchMapping("/platos/{idPlato}/estado")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDishStatus(@PathVariable Long idPlato, @RequestParam boolean activo) {
        plazoletaService.setDishActive(idPlato, activo);
    }

    @GetMapping("/restaurantes")
    @PreAuthorize("hasRole('CLIENTE')")
    public Page<RestaurantCardResponse> listRestaurants(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return plazoletaService.listRestaurants(PageRequest.of(page, size));
    }

    @GetMapping("/restaurantes/{idRestaurante}/platos")
    @PreAuthorize("hasRole('CLIENTE')")
    public Page<DishResponse> listDishes(@PathVariable Long idRestaurante,
                                         @RequestParam(required = false) String categoria,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return plazoletaService.listDishes(idRestaurante, categoria, PageRequest.of(page, size));
    }

    @PostMapping("/pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        return plazoletaService.createOrder(request);
    }

    @GetMapping("/pedidos")
    @PreAuthorize("hasRole('EMPLEADO')")
    public Page<OrderResponse> listOrdersByStatus(@RequestParam EstadoPedido estado,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return plazoletaService.listOrdersByStatus(estado, PageRequest.of(page, size));
    }

    @PatchMapping("/pedidos/{idPedido}/asignar")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse takeOrder(@PathVariable Long idPedido) {
        return plazoletaService.takeOrder(idPedido);
    }

    @PatchMapping("/pedidos/{idPedido}/listo")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse markReady(@PathVariable Long idPedido) {
        return plazoletaService.markReady(idPedido);
    }

    @PatchMapping("/pedidos/{idPedido}/entregar")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse deliverOrder(@PathVariable Long idPedido, @RequestBody DeliverOrderRequest request) {
        return plazoletaService.deliverOrder(idPedido, request);
    }

    @PatchMapping("/pedidos/{idPedido}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public OrderResponse cancelOrder(@PathVariable Long idPedido) {
        return plazoletaService.cancelOrder(idPedido);
    }

    @GetMapping("/pedidos/{idPedido}/trazabilidad")
    @PreAuthorize("hasRole('CLIENTE')")
    public Object trace(@PathVariable Long idPedido) {
        return plazoletaService.getTrace(idPedido);
    }

    @GetMapping("/pedidos/eficiencia")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public EficienciaResponse efficiency() {
        return plazoletaService.getEfficiency();
    }
}
