package com.pragma.powerup.plazoleta.application.handler;

import com.pragma.powerup.plazoleta.application.dto.request.CreateOrderRequest;
import com.pragma.powerup.plazoleta.application.dto.request.DeliverOrderRequest;
import com.pragma.powerup.plazoleta.application.dto.response.EficienciaResponse;
import com.pragma.powerup.plazoleta.application.dto.response.OrderResponse;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;

public interface IOrderHandler {

    OrderResponse createOrder(CreateOrderRequest request, Long clientId);

    PageResult<OrderResponse> listOrdersByStatus(EstadoPedidoModel estado, int page, int size, Long employeeId);

    OrderResponse takeOrder(Long idPedido, Long employeeId);

    OrderResponse markReady(Long idPedido, Long employeeId);

    OrderResponse deliverOrder(Long idPedido, DeliverOrderRequest request, Long employeeId);

    OrderResponse cancelOrder(Long idPedido, Long clientId);

    Object trace(Long idPedido);

    EficienciaResponse efficiency(Long ownerId);
}
