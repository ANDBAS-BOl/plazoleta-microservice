package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.api.PlazoletaUseCasePort;
import com.pragma.powerup.plazoleta.domain.spi.PlazoletaPersistencePort;

public class PlazoletaUseCase implements PlazoletaUseCasePort {

    private final PlazoletaPersistencePort plazoletaPersistencePort;

    public PlazoletaUseCase(PlazoletaPersistencePort plazoletaPersistencePort) {
        this.plazoletaPersistencePort = plazoletaPersistencePort;
    }

    @Override
    public void validateScaffoldingReady() {
        plazoletaPersistencePort.validatePersistenceWiring();
    }
}
