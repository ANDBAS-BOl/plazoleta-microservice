package com.pragma.powerup.plazoleta.infrastructure.configuration;

import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.OrderEfficiencyPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderMessagingPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.OrderPinGeneratorPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceQueryPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceabilityPort;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;
import com.pragma.powerup.plazoleta.domain.usecase.CatalogUseCase;
import com.pragma.powerup.plazoleta.domain.usecase.OrderEfficiencyUseCase;
import com.pragma.powerup.plazoleta.domain.usecase.OrderTraceQueryUseCase;
import com.pragma.powerup.plazoleta.domain.usecase.OrderUseCase;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.OrderMessagingAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.OrderTraceQueryAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.OrderTraceabilityAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.UsuariosValidationAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.MensajeriaClient;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.TrazabilidadClient;
import com.pragma.powerup.plazoleta.infrastructure.out.http.client.UsuariosClient;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter.CatalogJpaAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter.OrderEfficiencyJpaAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter.OrderJpaAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.DishRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.OrderRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.repository.RestaurantRepository;
import com.pragma.powerup.plazoleta.infrastructure.out.pin.RandomPinGeneratorAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlazoletaHexagonalBeanConfiguration {

    @Bean
    public CatalogPersistencePort catalogPersistencePort(RestaurantRepository restaurantRepository,
                                                         DishRepository dishRepository,
                                                         EmployeeRestaurantRepository employeeRestaurantRepository) {
        return new CatalogJpaAdapter(restaurantRepository, dishRepository, employeeRestaurantRepository);
    }

    @Bean
    public UsuariosValidationPort usuariosValidationPort(UsuariosClient usuariosClient) {
        return new UsuariosValidationAdapter(usuariosClient);
    }

    @Bean
    public CatalogUseCasePort catalogUseCasePort(CatalogPersistencePort catalogPersistencePort,
                                                 UsuariosValidationPort usuariosValidationPort) {
        return new CatalogUseCase(catalogPersistencePort, usuariosValidationPort);
    }

    @Bean
    public OrderPersistencePort orderPersistencePort(RestaurantRepository restaurantRepository,
                                                     DishRepository dishRepository,
                                                     OrderRepository orderRepository,
                                                     EmployeeRestaurantRepository employeeRestaurantRepository) {
        return new OrderJpaAdapter(
                restaurantRepository,
                dishRepository,
                orderRepository,
                employeeRestaurantRepository);
    }

    @Bean
    public OrderTraceabilityPort orderTraceabilityPort(TrazabilidadClient trazabilidadClient) {
        return new OrderTraceabilityAdapter(trazabilidadClient);
    }

    @Bean
    public OrderTraceQueryPort orderTraceQueryPort(TrazabilidadClient trazabilidadClient) {
        return new OrderTraceQueryAdapter(trazabilidadClient);
    }

    @Bean
    public OrderTraceQueryUseCasePort orderTraceQueryUseCasePort(OrderTraceQueryPort orderTraceQueryPort) {
        return new OrderTraceQueryUseCase(orderTraceQueryPort);
    }

    @Bean
    public OrderMessagingPort orderMessagingPort(MensajeriaClient mensajeriaClient) {
        return new OrderMessagingAdapter(mensajeriaClient);
    }

    @Bean
    public OrderPinGeneratorPort orderPinGeneratorPort() {
        return new RandomPinGeneratorAdapter();
    }

    @Bean
    public OrderEfficiencyPort orderEfficiencyPort(RestaurantRepository restaurantRepository,
                                                   OrderRepository orderRepository) {
        return new OrderEfficiencyJpaAdapter(restaurantRepository, orderRepository);
    }

    @Bean
    public OrderEfficiencyUseCasePort orderEfficiencyUseCasePort(OrderEfficiencyPort orderEfficiencyPort) {
        return new OrderEfficiencyUseCase(orderEfficiencyPort);
    }

    @Bean
    public OrderUseCasePort orderUseCasePort(OrderPersistencePort orderPersistencePort,
                                             OrderTraceabilityPort orderTraceabilityPort,
                                             OrderMessagingPort orderMessagingPort,
                                             OrderPinGeneratorPort orderPinGeneratorPort) {
        return new OrderUseCase(
                orderPersistencePort,
                orderTraceabilityPort,
                orderMessagingPort,
                orderPinGeneratorPort);
    }
}
