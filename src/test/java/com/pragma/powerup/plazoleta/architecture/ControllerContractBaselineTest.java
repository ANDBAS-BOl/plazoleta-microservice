package com.pragma.powerup.plazoleta.architecture;

import com.pragma.powerup.plazoleta.infrastructure.input.rest.CatalogRestController;
import com.pragma.powerup.plazoleta.infrastructure.input.rest.OrderRestController;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ControllerContractBaselineTest {

    private static final String BASE_PATH = "/api/v1/plazoleta";

    @Test
    void shouldKeepAllEndpointContracts() {
        Map<String, EndpointContract> expected = expectedContracts();
        Map<String, EndpointContract> actual = new HashMap<>();

        collectContracts(CatalogRestController.class, actual);
        collectContracts(OrderRestController.class, actual);

        assertEquals(expected, actual, "Cambio detectado en contratos HTTP de plazoleta");
    }

    @Test
    void shouldKeepExplicitResponseStatusForCreatedAndNoContentEndpoints() {
        assertMethodStatus(CatalogRestController.class, "createRestaurant", HttpStatus.CREATED);
        assertMethodStatus(CatalogRestController.class, "createDish", HttpStatus.CREATED);
        assertMethodStatus(CatalogRestController.class, "assignEmployeeToRestaurant", HttpStatus.CREATED);
        assertMethodStatus(CatalogRestController.class, "updateDish", HttpStatus.NO_CONTENT);
        assertMethodStatus(CatalogRestController.class, "changeDishStatus", HttpStatus.NO_CONTENT);
        assertMethodStatus(OrderRestController.class, "createOrder", HttpStatus.CREATED);
    }

    private void collectContracts(Class<?> controllerClass, Map<String, EndpointContract> target) {
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (method.isAnnotationPresent(ExceptionHandler.class)) {
                continue;
            }
            if (method.isSynthetic()) {
                continue;
            }
            EndpointContract contract = toContract(method);
            if (contract != null) {
                target.put(method.getName(), contract);
            }
        }
    }

    private void assertMethodStatus(Class<?> controllerClass, String methodName, HttpStatus expectedStatus) {
        Method method = findMethod(controllerClass, methodName);
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);
        assertNotNull(status, "Falta @ResponseStatus en " + methodName);
        assertEquals(expectedStatus, status.value(), "Status inesperado en " + methodName);
    }

    private Method findMethod(Class<?> controllerClass, String methodName) {
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Metodo no encontrado: " + methodName + " en " + controllerClass.getSimpleName());
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
            return null;
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
