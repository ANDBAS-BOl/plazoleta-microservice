package com.pragma.powerup.plazoleta.infrastructure.configuration;

import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.PlazoletaUseCasePort;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.OrderMessagingPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.OrderPinGeneratorPort;
import com.pragma.powerup.plazoleta.domain.spi.OrderTraceabilityPort;
import com.pragma.powerup.plazoleta.domain.spi.PlazoletaPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;
import com.pragma.powerup.plazoleta.domain.usecase.CatalogUseCase;
import com.pragma.powerup.plazoleta.domain.usecase.OrderUseCase;
import com.pragma.powerup.plazoleta.domain.usecase.PlazoletaUseCase;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.OrderMessagingAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.OrderTraceabilityAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.UsuariosValidationAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter.CatalogJpaAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter.OrderJpaAdapter;
import com.pragma.powerup.plazoleta.client.MensajeriaClient;
import com.pragma.powerup.plazoleta.client.TrazabilidadClient;
import com.pragma.powerup.plazoleta.repository.DishRepository;
import com.pragma.powerup.plazoleta.repository.EmployeeRestaurantRepository;
import com.pragma.powerup.plazoleta.repository.OrderRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
import com.pragma.powerup.plazoleta.service.pin.PinGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlazoletaHexagonalBeanConfiguration {

    @Bean
    public PlazoletaPersistencePort plazoletaPersistencePort() {
        return () -> {
            // Placeholder adapter for phase 1 scaffolding.
        };
    }

    @Bean
    public PlazoletaUseCasePort plazoletaUseCasePort(PlazoletaPersistencePort plazoletaPersistencePort) {
        return new PlazoletaUseCase(plazoletaPersistencePort);
    }

    @Bean
    public CatalogPersistencePort catalogPersistencePort(RestaurantRepository restaurantRepository,
                                                         DishRepository dishRepository) {
        return new CatalogJpaAdapter(restaurantRepository, dishRepository);
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
    public OrderMessagingPort orderMessagingPort(MensajeriaClient mensajeriaClient) {
        return new OrderMessagingAdapter(mensajeriaClient);
    }

    @Bean
    public OrderPinGeneratorPort orderPinGeneratorPort(PinGenerator pinGenerator) {
        return pinGenerator::generarPin6Digitos;
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
