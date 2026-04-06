package com.pragma.powerup.plazoleta.infrastructure.input.rest;

import com.pragma.powerup.plazoleta.application.dto.request.CreateOrderRequest;
import com.pragma.powerup.plazoleta.application.dto.request.DeliverOrderRequest;
import com.pragma.powerup.plazoleta.application.dto.response.EficienciaResponse;
import com.pragma.powerup.plazoleta.application.dto.response.OrderResponse;
import com.pragma.powerup.plazoleta.application.handler.IOrderHandler;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.infrastructure.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plazoleta")
@RequiredArgsConstructor
public class OrderRestController {

    private final IOrderHandler orderHandler;

    @PostMapping("/pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request,
                                     @AuthenticationPrincipal UsuarioPrincipal principal) {
        return orderHandler.createOrder(request, principal.getId());
    }

    @GetMapping("/pedidos")
    @PreAuthorize("hasRole('EMPLEADO')")
    public Page<OrderResponse> listOrdersByStatus(@RequestParam EstadoPedidoModel estado,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        PageResult<OrderResponse> result = orderHandler.listOrdersByStatus(estado, page, size, principal.getId());
        return new PageImpl<>(result.content(), PageRequest.of(page, size), result.totalElements());
    }

    @PatchMapping("/pedidos/{idPedido}/asignar")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse takeOrder(@PathVariable Long idPedido,
                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        return orderHandler.takeOrder(idPedido, principal.getId());
    }

    @PatchMapping("/pedidos/{idPedido}/listo")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse markReady(@PathVariable Long idPedido,
                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        return orderHandler.markReady(idPedido, principal.getId());
    }

    @PatchMapping("/pedidos/{idPedido}/entregar")
    @PreAuthorize("hasRole('EMPLEADO')")
    public OrderResponse deliverOrder(@PathVariable Long idPedido,
                                      @RequestBody DeliverOrderRequest request,
                                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        return orderHandler.deliverOrder(idPedido, request, principal.getId());
    }

    @PatchMapping("/pedidos/{idPedido}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public OrderResponse cancelOrder(@PathVariable Long idPedido,
                                     @AuthenticationPrincipal UsuarioPrincipal principal) {
        return orderHandler.cancelOrder(idPedido, principal.getId());
    }

    @GetMapping("/pedidos/{idPedido}/trazabilidad")
    @PreAuthorize("hasRole('CLIENTE')")
    public Object trace(@PathVariable Long idPedido) {
        return orderHandler.trace(idPedido);
    }

    @GetMapping("/pedidos/eficiencia")
    @PreAuthorize("hasRole('PROPIETARIO')")
    public EficienciaResponse efficiency(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return orderHandler.efficiency(principal.getId());
    }
}
