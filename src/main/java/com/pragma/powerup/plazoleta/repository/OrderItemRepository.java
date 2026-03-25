package com.pragma.powerup.plazoleta.repository;

import com.pragma.powerup.plazoleta.domain.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
}
