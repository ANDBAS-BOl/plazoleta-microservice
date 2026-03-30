package com.pragma.powerup.plazoleta.architecture;

import com.pragma.powerup.plazoleta.domain.api.PlazoletaUseCasePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PlazoletaHexagonalScaffoldingTest {

    @Autowired
    private PlazoletaUseCasePort plazoletaUseCasePort;

    @Test
    void shouldLoadHexagonalScaffoldingBeans() {
        assertThat(plazoletaUseCasePort).isNotNull();
        assertThatNoException().isThrownBy(plazoletaUseCasePort::validateScaffoldingReady);
    }
}
