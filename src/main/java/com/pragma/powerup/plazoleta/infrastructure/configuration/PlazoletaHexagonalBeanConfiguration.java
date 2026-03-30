package com.pragma.powerup.plazoleta.infrastructure.configuration;

import com.pragma.powerup.plazoleta.client.UsuariosClient;
import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.PlazoletaUseCasePort;
import com.pragma.powerup.plazoleta.domain.spi.CatalogPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.PlazoletaPersistencePort;
import com.pragma.powerup.plazoleta.domain.spi.UsuariosValidationPort;
import com.pragma.powerup.plazoleta.domain.usecase.CatalogUseCase;
import com.pragma.powerup.plazoleta.domain.usecase.PlazoletaUseCase;
import com.pragma.powerup.plazoleta.infrastructure.out.http.adapter.UsuariosValidationAdapter;
import com.pragma.powerup.plazoleta.infrastructure.out.jpa.adapter.CatalogJpaAdapter;
import com.pragma.powerup.plazoleta.repository.DishRepository;
import com.pragma.powerup.plazoleta.repository.RestaurantRepository;
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
}
