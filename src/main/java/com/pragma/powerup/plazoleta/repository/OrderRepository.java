package com.pragma.powerup.plazoleta.repository;

import com.pragma.powerup.plazoleta.domain.EstadoPedido;
import com.pragma.powerup.plazoleta.domain.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    boolean existsByIdClienteAndEstadoIn(Long idCliente, Collection<EstadoPedido> estados);

    Page<OrderEntity> findByRestaurantIdAndEstado(Long restaurantId, EstadoPedido estado, Pageable pageable);

    List<OrderEntity> findByRestaurantIdInAndEstado(Collection<Long> restaurantIds, EstadoPedido estado);

    boolean existsByPinSeguridad(String pinSeguridad);

    @Modifying
    @Query("update OrderEntity o set o.estado = :newState, o.idEmpleadoAsignado = :idEmpleado " +
            "where o.id = :id and o.estado = :expectedState and o.idEmpleadoAsignado is null")
    int takeOrderIfPending(@Param("id") Long id,
                           @Param("expectedState") EstadoPedido expectedState,
                           @Param("newState") EstadoPedido newState,
                           @Param("idEmpleado") Long idEmpleado);
}
