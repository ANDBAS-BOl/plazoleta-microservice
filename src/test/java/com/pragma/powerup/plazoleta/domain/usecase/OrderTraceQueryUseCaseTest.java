package com.pragma.powerup.plazoleta.domain.usecase;

import com.pragma.powerup.plazoleta.domain.spi.OrderTraceQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderTraceQueryUseCaseTest {

    private OrderTraceQueryPort traceQueryPort;
    private OrderTraceQueryUseCase useCase;

    @BeforeEach
    void setUp() {
        traceQueryPort = mock(OrderTraceQueryPort.class);
        useCase = new OrderTraceQueryUseCase(traceQueryPort);
    }

    @Test
    void getTraceByOrderIdShouldDelegateToPort() {
        List<Map<String, Object>> trace = List.of(
                Map.of("estadoAnterior", "null", "estadoNuevo", "PENDIENTE"),
                Map.of("estadoAnterior", "PENDIENTE", "estadoNuevo", "EN_PREPARACION")
        );
        when(traceQueryPort.getTraceByOrderId(100L)).thenReturn(trace);

        Object result = useCase.getTraceByOrderId(100L);

        assertNotNull(result);
        assertEquals(trace, result);
        verify(traceQueryPort).getTraceByOrderId(100L);
    }

    @Test
    void getTraceByOrderIdShouldReturnEmptyWhenNoEvents() {
        when(traceQueryPort.getTraceByOrderId(999L)).thenReturn(List.of());

        Object result = useCase.getTraceByOrderId(999L);

        assertNotNull(result);
        assertEquals(List.of(), result);
    }
}
