package com.pragma.powerup.plazoleta.domain.api;

import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.domain.model.PaginationParams;

import java.util.List;

public interface OrderUseCasePort {

    OrderModel createOrder(Long clientId, Long restaurantId, String telefonoCliente, List<OrderItemModel> items);

    PageResult<OrderModel> listOrdersByStatus(Long employeeId, EstadoPedidoModel estado, PaginationParams pagination);

    OrderModel takeOrder(Long idOrder, Long employeeId);

    OrderModel markReady(Long idOrder, Long employeeId);

    OrderModel deliverOrder(Long idOrder, Long employeeId, String pin);

    OrderModel cancelOrder(Long idOrder, Long clientId);
}
