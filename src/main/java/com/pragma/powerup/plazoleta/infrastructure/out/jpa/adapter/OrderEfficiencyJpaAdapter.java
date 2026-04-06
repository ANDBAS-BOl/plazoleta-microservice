package com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter;

import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyEmployeeModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;
import com.pragma.powerup.plazoleta.domain.spi.OrderEfficiencyPort;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.EstadoPedido;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.OrderRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.RestaurantRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderEfficiencyJpaAdapter implements OrderEfficiencyPort {

    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;

    public OrderEfficiencyJpaAdapter(RestaurantRepository restaurantRepository, OrderRepository orderRepository) {
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderEfficiencyModel calculateOwnerEfficiency(Long ownerId) {
        List<Long> restaurantIds = restaurantRepository.findAll().stream()
                .filter(r -> r.getIdPropietario().equals(ownerId))
                .map(RestaurantEntity::getId)
                .toList();
        if (restaurantIds.isEmpty()) {
            return new OrderEfficiencyModel(List.of());
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

        List<OrderEfficiencyEmployeeModel> ranking = minutesByEmployee.entrySet().stream()
                .map(e -> new OrderEfficiencyEmployeeModel(
                        e.getKey(),
                        e.getValue().stream().mapToLong(v -> v).average().orElse(0)))
                .sorted(Comparator.comparing(OrderEfficiencyEmployeeModel::tiempoPromedioMinutos))
                .toList();
        return new OrderEfficiencyModel(ranking);
    }
}
