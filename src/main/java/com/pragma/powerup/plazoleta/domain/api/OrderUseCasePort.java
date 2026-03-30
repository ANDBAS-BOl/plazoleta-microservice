package com.pragma.powerup.plazoleta.domain.api;

import com.pragma.powerup.plazoleta.domain.model.EstadoPedidoModel;
import com.pragma.powerup.plazoleta.domain.model.OrderItemModel;
import com.pragma.powerup.plazoleta.domain.model.OrderModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderUseCasePort {

    OrderModel createOrder(Long clientId, Long restaurantId, String telefonoCliente, List<OrderItemModel> items);

    Page<OrderModel> listOrdersByStatus(Long employeeId, EstadoPedidoModel estado, Pageable pageable);

    OrderModel takeOrder(Long idOrder, Long employeeId);

    OrderModel markReady(Long idOrder, Long employeeId);

    OrderModel deliverOrder(Long idOrder, Long employeeId, String pin);

    OrderModel cancelOrder(Long idOrder, Long clientId);
}
