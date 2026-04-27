package com.pragma.powerup.plazoleta.application.handler.impl;

import com.pragma.powerup.plazoleta.application.dto.request.CreateOrderRequest;
import com.pragma.powerup.plazoleta.application.dto.request.DeliverOrderRequest;
import com.pragma.powerup.plazoleta.application.dto.response.EficienciaResponse;
import com.pragma.powerup.plazoleta.application.dto.response.OrderResponse;
import com.pragma.powerup.plazoleta.application.handler.IOrderHandler;
import com.pragma.powerup.plazoleta.application.mapper.IPlazoletaDtoMapper;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderHandler implements IOrderHandler {

    private final OrderUseCasePort orderUseCasePort;
    private final OrderTraceQueryUseCasePort orderTraceQueryUseCasePort;
    private final OrderEfficiencyUseCasePort orderEfficiencyUseCasePort;
    private final IPlazoletaDtoMapper plazoletaDtoMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Long clientId) {
        OrderModel model = orderUseCasePort.createOrder(
                clientId,
                request.idRestaurante(),
                request.telefonoCliente(),
                plazoletaDtoMapper.toOrderItems(request));
        return plazoletaDtoMapper.toOrderResponse(model);
    }

    @Override
    public PageResult<OrderResponse> listOrdersByStatus(EstadoPedidoModel estado, int page, int size,
                                                        Long employeeId) {
        return orderUseCasePort
                .listOrdersByStatus(employeeId, estado, new PaginationParams(page, size))
                .map(plazoletaDtoMapper::toOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse takeOrder(Long idPedido, Long employeeId) {
        return plazoletaDtoMapper.toOrderResponse(orderUseCasePort.takeOrder(idPedido, employeeId));
    }

    @Override
    @Transactional
    public OrderResponse markReady(Long idPedido, Long employeeId) {
        return plazoletaDtoMapper.toOrderResponse(orderUseCasePort.markReady(idPedido, employeeId));
    }

    @Override
    @Transactional
    public OrderResponse deliverOrder(Long idPedido, DeliverOrderRequest request, Long employeeId) {
        return plazoletaDtoMapper.toOrderResponse(
                orderUseCasePort.deliverOrder(idPedido, employeeId, request.pin()));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long idPedido, Long clientId) {
        return plazoletaDtoMapper.toOrderResponse(orderUseCasePort.cancelOrder(idPedido, clientId));
    }

    @Override
    public Object trace(Long idPedido) {
        return orderTraceQueryUseCasePort.getTraceByOrderId(idPedido);
    }

    @Override
    public EficienciaResponse efficiency(Long ownerId) {
        return plazoletaDtoMapper.toEficienciaResponse(
                orderEfficiencyUseCasePort.getOwnerEfficiency(ownerId));
    }
}
