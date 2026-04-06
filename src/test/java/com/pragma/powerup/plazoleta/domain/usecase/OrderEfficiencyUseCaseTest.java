package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyEmployeeModel;
import com.pragma.powerup.plazoleta.domain.model.OrderEfficiencyModel;
import com.pragma.powerup.plazoleta.domain.spi.OrderEfficiencyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderEfficiencyUseCaseTest {

    private OrderEfficiencyPort efficiencyPort;
    private OrderEfficiencyUseCase useCase;

    @BeforeEach
    void setUp() {
        efficiencyPort = mock(OrderEfficiencyPort.class);
        useCase = new OrderEfficiencyUseCase(efficiencyPort);
    }

    @Test
    void getOwnerEfficiencyShouldDelegateToPort() {
        List<OrderEfficiencyEmployeeModel> ranking = List.of(
                new OrderEfficiencyEmployeeModel(100L, 12.5),
                new OrderEfficiencyEmployeeModel(200L, 18.3)
        );
        OrderEfficiencyModel expected = new OrderEfficiencyModel(ranking);
        when(efficiencyPort.calculateOwnerEfficiency(1L)).thenReturn(expected);

        OrderEfficiencyModel result = useCase.getOwnerEfficiency(1L);

        assertNotNull(result);
        assertEquals(2, result.ranking().size());
        assertEquals(100L, result.ranking().get(0).idEmpleado());
        assertEquals(12.5, result.ranking().get(0).tiempoPromedioMinutos());
        assertEquals(200L, result.ranking().get(1).idEmpleado());
        verify(efficiencyPort).calculateOwnerEfficiency(1L);
    }

    @Test
    void getOwnerEfficiencyShouldReturnEmptyRankingWhenNoDelivered() {
        OrderEfficiencyModel empty = new OrderEfficiencyModel(List.of());
        when(efficiencyPort.calculateOwnerEfficiency(1L)).thenReturn(empty);

        OrderEfficiencyModel result = useCase.getOwnerEfficiency(1L);

        assertNotNull(result);
        assertTrue(result.ranking().isEmpty());
    }

    @Test
    void getOwnerEfficiencyShouldOnlyIncludeOwnerRestaurants() {
        List<OrderEfficiencyEmployeeModel> ranking = List.of(
                new OrderEfficiencyEmployeeModel(300L, 10.0)
        );
        OrderEfficiencyModel expected = new OrderEfficiencyModel(ranking);
        when(efficiencyPort.calculateOwnerEfficiency(5L)).thenReturn(expected);

        OrderEfficiencyModel result = useCase.getOwnerEfficiency(5L);

        assertEquals(1, result.ranking().size());
        verify(efficiencyPort).calculateOwnerEfficiency(5L);
        verify(efficiencyPort, never()).calculateOwnerEfficiency(argThat(id -> !id.equals(5L)));
    }
}
