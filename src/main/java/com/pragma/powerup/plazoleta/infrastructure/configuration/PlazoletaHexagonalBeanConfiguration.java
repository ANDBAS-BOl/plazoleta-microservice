package com.pragma.powerup.plazoleta.infrastructure.configuration;

import com.pragma.powerup.plazoleta.domain.api.PlazoletaUseCasePort;
import com.pragma.powerup.plazoleta.domain.spi.PlazoletaPersistencePort;
import com.pragma.powerup.plazoleta.domain.usecase.PlazoletaUseCase;
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
}
