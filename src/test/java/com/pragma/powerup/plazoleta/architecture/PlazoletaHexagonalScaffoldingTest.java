package com.pragma.powerup.plazoleta.architecture;

import com.pragma.powerup.plazoleta.domain.api.CatalogUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderEfficiencyUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderTraceQueryUseCasePort;
import com.pragma.powerup.plazoleta.domain.api.OrderUseCasePort;
import com.pragma.powerup.plazoleta.application.handler.ICatalogHandler;
import com.pragma.powerup.plazoleta.application.handler.IOrderHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PlazoletaHexagonalScaffoldingTest {

    @Autowired
    private CatalogUseCasePort catalogUseCasePort;

    @Autowired
    private OrderUseCasePort orderUseCasePort;

    @Autowired
    private OrderEfficiencyUseCasePort orderEfficiencyUseCasePort;

    @Autowired
    private OrderTraceQueryUseCasePort orderTraceQueryUseCasePort;

    @Autowired
    private ICatalogHandler catalogHandler;

    @Autowired
    private IOrderHandler orderHandler;

    @Test
    void shouldLoadAllHexagonalBeans() {
        assertThat(catalogUseCasePort).isNotNull();
        assertThat(orderUseCasePort).isNotNull();
        assertThat(orderEfficiencyUseCasePort).isNotNull();
        assertThat(orderTraceQueryUseCasePort).isNotNull();
        assertThat(catalogHandler).isNotNull();
        assertThat(orderHandler).isNotNull();
    }
}
