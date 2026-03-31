package com.pragma.powerup.plazoleta.web;

import com.pragma.powerup.plazoleta.infrastructure.input.rest.PlazoletaController;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class PlazoletaControllerContractBaselineTest {

    private static final String BASE_PATH = "/api/v1/plazoleta";

    @Test
    void shouldKeepControllerEndpointContracts() {
        Map<String, EndpointContract> expected = expectedContracts();
        Map<String, EndpointContract> actual = new HashMap<>();

        for (Method method : PlazoletaController.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            EndpointContract contract = toContract(method);
            actual.put(method.getName(), contract);
        }

        assertEquals(expected, actual, "Cambio detectado en contratos de PlazoletaController");
    }

    @Test
    void shouldKeepExplicitResponseStatusForCreatedAndNoContentEndpoints() throws NoSuchMethodException {
        assertMethodStatus("createRestaurant", HttpStatus.CREATED);
        assertMethodStatus("createDish", HttpStatus.CREATED);
        assertMethodStatus("assignEmployeeToRestaurant", HttpStatus.CREATED);
        assertMethodStatus("updateDish", HttpStatus.NO_CONTENT);
        assertMethodStatus("changeDishStatus", HttpStatus.NO_CONTENT);
        assertMethodStatus("createOrder", HttpStatus.CREATED);
    }

    private void assertMethodStatus(String methodName, HttpStatus expectedStatus) throws NoSuchMethodException {
        Method method = findMethod(methodName);
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);
        assertNotNull(status, "Falta @ResponseStatus en " + methodName);
        assertEquals(expectedStatus, status.value(), "Status inesperado en " + methodName);
    }

    private Method findMethod(String methodName) throws NoSuchMethodException {
        for (Method method : PlazoletaController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private EndpointContract toContract(Method method) {
        String httpMethod;
        String relativePath;

        PostMapping post = method.getAnnotation(PostMapping.class);
        GetMapping get = method.getAnnotation(GetMapping.class);
        PatchMapping patch = method.getAnnotation(PatchMapping.class);

        if (post != null) {
            httpMethod = "POST";
            relativePath = firstPath(post.value());
        } else if (get != null) {
            httpMethod = "GET";
            relativePath = firstPath(get.value());
        } else if (patch != null) {
            httpMethod = "PATCH";
            relativePath = firstPath(patch.value());
        } else {
            throw new IllegalStateException("Metodo sin mapping HTTP: " + method.getName());
        }

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        String role = preAuthorize == null ? "" : preAuthorize.value();

        return new EndpointContract(httpMethod, BASE_PATH + relativePath, role);
    }

    private String firstPath(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        return values[0];
    }

    private Map<String, EndpointContract> expectedContracts() {
        return Map.ofEntries(
                Map.entry("createRestaurant", new EndpointContract("POST", "/api/v1/plazoleta/restaurantes", "hasRole('ADMINISTRADOR')")),
                Map.entry("createDish", new EndpointContract("POST", "/api/v1/plazoleta/platos", "hasRole('PROPIETARIO')")),
                Map.entry("assignEmployeeToRestaurant", new EndpointContract("POST", "/api/v1/plazoleta/restaurantes/{idRestaurante}/empleados", "hasRole('PROPIETARIO')")),
                Map.entry("updateDish", new EndpointContract("PATCH", "/api/v1/plazoleta/platos/{idPlato}", "hasRole('PROPIETARIO')")),
                Map.entry("changeDishStatus", new EndpointContract("PATCH", "/api/v1/plazoleta/platos/{idPlato}/estado", "hasRole('PROPIETARIO')")),
                Map.entry("listRestaurants", new EndpointContract("GET", "/api/v1/plazoleta/restaurantes", "hasRole('CLIENTE')")),
                Map.entry("listDishes", new EndpointContract("GET", "/api/v1/plazoleta/restaurantes/{idRestaurante}/platos", "hasRole('CLIENTE')")),
                Map.entry("createOrder", new EndpointContract("POST", "/api/v1/plazoleta/pedidos", "hasRole('CLIENTE')")),
                Map.entry("listOrdersByStatus", new EndpointContract("GET", "/api/v1/plazoleta/pedidos", "hasRole('EMPLEADO')")),
                Map.entry("takeOrder", new EndpointContract("PATCH", "/api/v1/plazoleta/pedidos/{idPedido}/asignar", "hasRole('EMPLEADO')")),
                Map.entry("markReady", new EndpointContract("PATCH", "/api/v1/plazoleta/pedidos/{idPedido}/listo", "hasRole('EMPLEADO')")),
                Map.entry("deliverOrder", new EndpointContract("PATCH", "/api/v1/plazoleta/pedidos/{idPedido}/entregar", "hasRole('EMPLEADO')")),
                Map.entry("cancelOrder", new EndpointContract("PATCH", "/api/v1/plazoleta/pedidos/{idPedido}/cancelar", "hasRole('CLIENTE')")),
                Map.entry("trace", new EndpointContract("GET", "/api/v1/plazoleta/pedidos/{idPedido}/trazabilidad", "hasRole('CLIENTE')")),
                Map.entry("efficiency", new EndpointContract("GET", "/api/v1/plazoleta/pedidos/eficiencia", "hasRole('PROPIETARIO')"))
        );
    }

    private record EndpointContract(String httpMethod, String path, String role) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof EndpointContract other)) {
                return false;
            }
            return Objects.equals(httpMethod, other.httpMethod)
                    && Objects.equals(path, other.path)
                    && Objects.equals(role, other.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(httpMethod, path, role);
        }
    }
}
