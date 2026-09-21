package com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository;

import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.EstadoPedido;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.OrderEntity;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.entity.RestaurantEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Test
    void deliverOrderIfListoAndPinShouldConsumePinAndSetDeliveredState() {
        RestaurantEntity restaurant = createRestaurant();

        OrderEntity order = new OrderEntity();
        order.setRestaurant(restaurant);
        order.setIdCliente(50L);
        order.setTelefonoCliente("3001234567");
        order.setEstado(EstadoPedido.LISTO);
        order.setFechaCreacion(LocalDateTime.now().minusMinutes(15));
        order.setIdEmpleadoAsignado(100L);
        order.setPinSeguridad("123456");
        OrderEntity savedOrder = orderRepository.saveAndFlush(order);

        int affectedRows = orderRepository.deliverOrderIfListoAndPin(
                savedOrder.getId(),
                EstadoPedido.LISTO,
                EstadoPedido.ENTREGADO,
                100L,
                "123456"
        );

        OrderEntity updated = orderRepository.findById(savedOrder.getId()).orElseThrow();

        assertEquals(1, affectedRows);
        assertEquals(EstadoPedido.ENTREGADO, updated.getEstado());
        assertNull(updated.getPinSeguridad());
        assertNotNull(updated.getFechaEntrega());
    }

    @Test
    void deliverOrderIfListoAndPinShouldFailWhenTryingToReuseDeliveredOrder() {
        RestaurantEntity restaurant = createRestaurant();

        OrderEntity order = new OrderEntity();
        order.setRestaurant(restaurant);
        order.setIdCliente(60L);
        order.setTelefonoCliente("3009999999");
        order.setEstado(EstadoPedido.LISTO);
        order.setFechaCreacion(LocalDateTime.now().minusMinutes(20));
        order.setIdEmpleadoAsignado(101L);
        order.setPinSeguridad("654321");
        OrderEntity savedOrder = orderRepository.saveAndFlush(order);

        int firstDelivery = orderRepository.deliverOrderIfListoAndPin(
                savedOrder.getId(),
                EstadoPedido.LISTO,
                EstadoPedido.ENTREGADO,
                101L,
                "654321"
        );
        int secondDelivery = orderRepository.deliverOrderIfListoAndPin(
                savedOrder.getId(),
                EstadoPedido.LISTO,
                EstadoPedido.ENTREGADO,
                101L,
                "654321"
        );

        assertEquals(1, firstDelivery);
        assertEquals(0, secondDelivery);
    }

    private RestaurantEntity createRestaurant() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setNombre("Restaurante HU15");
        restaurant.setNit(String.valueOf(System.nanoTime()));
        restaurant.setDireccion("Calle 123");
        restaurant.setTelefono("+573001234567");
        restaurant.setUrlLogo("https://example.com/logo.png");
        restaurant.setIdPropietario(5L);
        return restaurantRepository.saveAndFlush(restaurant);
    }
}
