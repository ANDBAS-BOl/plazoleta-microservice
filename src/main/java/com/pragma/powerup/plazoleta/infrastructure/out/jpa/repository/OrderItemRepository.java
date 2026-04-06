package com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository;

import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
}
