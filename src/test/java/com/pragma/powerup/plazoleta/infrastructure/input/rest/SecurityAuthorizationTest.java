package com.pragma.powerup.plazoleta.infrastructure.input.rest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    private static final String SECRET = "test_jwt_secret_key_at_least_32_characters_long_for_hmac";
    private static final String BASE = "/api/v1/plazoleta";

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String propietarioToken;
    private String empleadoToken;
    private String clienteToken;

    @BeforeEach
    void setUp() {
        adminToken = generateToken(1L, "admin@test.com", "ADMINISTRADOR");
        propietarioToken = generateToken(2L, "owner@test.com", "PROPIETARIO");
        empleadoToken = generateToken(3L, "emp@test.com", "EMPLEADO");
        clienteToken = generateToken(4L, "client@test.com", "CLIENTE");
    }

    // ====================================================================
    // Sin token → debe rechazar (403) en todos los endpoints protegidos
    // ====================================================================

    @Nested
    class NoTokenTests {

        @Test
        void postRestaurantesSinToken() throws Exception {
            mockMvc.perform(post(BASE + "/restaurantes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void postPlatosSinToken() throws Exception {
            mockMvc.perform(post(BASE + "/platos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void postPedidosSinToken() throws Exception {
            mockMvc.perform(post(BASE + "/pedidos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getPedidosSinToken() throws Exception {
            mockMvc.perform(get(BASE + "/pedidos").param("estado", "PENDIENTE"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getRestaurantesSinToken() throws Exception {
            mockMvc.perform(get(BASE + "/restaurantes"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getEficienciaSinToken() throws Exception {
            mockMvc.perform(get(BASE + "/pedidos/eficiencia"))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // POST /restaurantes — solo ADMINISTRADOR
    // ====================================================================

    @Nested
    class CreateRestauranteSecurity {

        private static final String URL = BASE + "/restaurantes";
        private static final String BODY = """
                {"nombre":"Rest","nit":"123","direccion":"Dir","telefono":"+57300","urlLogo":"http://x","idPropietario":99}
                """;

        @Test
        void adminShouldPass() throws Exception {
            assertNotForbidden(post(URL).header("Authorization", bearer(adminToken))
                    .contentType(MediaType.APPLICATION_JSON).content(BODY));
        }

        @ParameterizedTest
        @ValueSource(strings = {"PROPIETARIO", "EMPLEADO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(post(URL).header("Authorization", bearer(tokenForRole(rol)))
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // POST /platos — solo PROPIETARIO
    // ====================================================================

    @Nested
    class CreatePlatoSecurity {

        private static final String URL = BASE + "/platos";
        private static final String BODY = """
                {"idRestaurante":1,"nombre":"Plato","precio":10000,"descripcion":"d","urlImagen":"http://x","categoria":"CAT"}
                """;

        @Test
        void propietarioShouldPass() throws Exception {
            assertNotForbidden(post(URL).header("Authorization", bearer(propietarioToken))
                    .contentType(MediaType.APPLICATION_JSON).content(BODY));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "EMPLEADO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(post(URL).header("Authorization", bearer(tokenForRole(rol)))
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // PATCH /platos/{id} — solo PROPIETARIO
    // ====================================================================

    @Nested
    class UpdatePlatoSecurity {

        private static final String URL = BASE + "/platos/1";

        @Test
        void propietarioShouldPass() throws Exception {
            assertNotForbidden(patch(URL).header("Authorization", bearer(propietarioToken))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"precio\":15000,\"descripcion\":\"x\"}"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "EMPLEADO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(patch(URL).header("Authorization", bearer(tokenForRole(rol)))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"precio\":15000,\"descripcion\":\"x\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // PATCH /platos/{id}/estado — solo PROPIETARIO
    // ====================================================================

    @Nested
    class ChangeDishStatusSecurity {

        private static final String URL = BASE + "/platos/1/estado";

        @Test
        void propietarioShouldPass() throws Exception {
            assertNotForbidden(patch(URL).param("activo", "false")
                    .header("Authorization", bearer(propietarioToken)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "EMPLEADO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(patch(URL).param("activo", "false")
                            .header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // POST /restaurantes/{id}/empleados — solo PROPIETARIO
    // ====================================================================

    @Nested
    class AssignEmployeeSecurity {

        private static final String URL = BASE + "/restaurantes/1/empleados";

        @Test
        void propietarioShouldPass() throws Exception {
            assertNotForbidden(post(URL).header("Authorization", bearer(propietarioToken))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"idEmpleado\":50}"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "EMPLEADO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(post(URL).header("Authorization", bearer(tokenForRole(rol)))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"idEmpleado\":50}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // GET /restaurantes — solo CLIENTE
    // ====================================================================

    @Nested
    class ListRestaurantesSecurity {

        private static final String URL = BASE + "/restaurantes";

        @Test
        void clienteShouldPass() throws Exception {
            mockMvc.perform(get(URL).header("Authorization", bearer(clienteToken)))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "EMPLEADO"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(get(URL).header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // GET /restaurantes/{id}/platos — solo CLIENTE
    // ====================================================================

    @Nested
    class ListPlatosSecurity {

        private static final String URL = BASE + "/restaurantes/1/platos";

        @Test
        void clienteShouldPass() throws Exception {
            mockMvc.perform(get(URL).header("Authorization", bearer(clienteToken)))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "EMPLEADO"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(get(URL).header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // POST /pedidos — solo CLIENTE
    // ====================================================================

    @Nested
    class CreateOrderSecurity {

        private static final String URL = BASE + "/pedidos";
        private static final String BODY = """
                {"idRestaurante":1,"telefonoCliente":"300","platos":[{"idPlato":1,"cantidad":1}]}
                """;

        @Test
        void clienteShouldPass() throws Exception {
            assertNotForbidden(post(URL).header("Authorization", bearer(clienteToken))
                    .contentType(MediaType.APPLICATION_JSON).content(BODY));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "EMPLEADO"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(post(URL).header("Authorization", bearer(tokenForRole(rol)))
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // GET /pedidos — solo EMPLEADO
    // ====================================================================

    @Nested
    class ListOrdersSecurity {

        private static final String URL = BASE + "/pedidos";

        @Test
        void empleadoShouldPass() throws Exception {
            assertNotForbidden(get(URL).param("estado", "PENDIENTE")
                    .header("Authorization", bearer(empleadoToken)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(get(URL).param("estado", "PENDIENTE")
                            .header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // PATCH /pedidos/{id}/asignar — solo EMPLEADO
    // ====================================================================

    @Nested
    class TakeOrderSecurity {

        private static final String URL = BASE + "/pedidos/1/asignar";

        @Test
        void empleadoShouldPass() throws Exception {
            assertNotForbidden(patch(URL).header("Authorization", bearer(empleadoToken)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(patch(URL).header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // PATCH /pedidos/{id}/listo — solo EMPLEADO
    // ====================================================================

    @Nested
    class MarkReadySecurity {

        private static final String URL = BASE + "/pedidos/1/listo";

        @Test
        void empleadoShouldPass() throws Exception {
            assertNotForbidden(patch(URL).header("Authorization", bearer(empleadoToken)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(patch(URL).header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // PATCH /pedidos/{id}/entregar — solo EMPLEADO
    // ====================================================================

    @Nested
    class DeliverOrderSecurity {

        private static final String URL = BASE + "/pedidos/1/entregar";

        @Test
        void empleadoShouldPass() throws Exception {
            assertNotForbidden(patch(URL).header("Authorization", bearer(empleadoToken))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"123456\"}"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(patch(URL).header("Authorization", bearer(tokenForRole(rol)))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"123456\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // PATCH /pedidos/{id}/cancelar — solo CLIENTE
    // ====================================================================

    @Nested
    class CancelOrderSecurity {

        private static final String URL = BASE + "/pedidos/1/cancelar";

        @Test
        void clienteShouldPass() throws Exception {
            assertNotForbidden(patch(URL).header("Authorization", bearer(clienteToken)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "EMPLEADO"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(patch(URL).header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // GET /pedidos/{id}/trazabilidad — solo CLIENTE
    // ====================================================================

    @Nested
    class TraceSecurity {

        private static final String URL = BASE + "/pedidos/1/trazabilidad";

        @Test
        void clienteShouldPass() throws Exception {
            assertNotForbidden(get(URL).header("Authorization", bearer(clienteToken)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "PROPIETARIO", "EMPLEADO"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(get(URL).header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // GET /pedidos/eficiencia — solo PROPIETARIO
    // ====================================================================

    @Nested
    class EfficiencySecurity {

        private static final String URL = BASE + "/pedidos/eficiencia";

        @Test
        void propietarioShouldPass() throws Exception {
            assertNotForbidden(get(URL).header("Authorization", bearer(propietarioToken)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ADMINISTRADOR", "EMPLEADO", "CLIENTE"})
        void otherRolesShouldBeForbidden(String rol) throws Exception {
            mockMvc.perform(get(URL).header("Authorization", bearer(tokenForRole(rol))))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // Token invalido/expirado
    // ====================================================================

    @Nested
    class InvalidTokenTests {

        @Test
        void expiredTokenShouldBeRejected() throws Exception {
            String expired = generateExpiredToken(1L, "a@test.com", "ADMINISTRADOR");
            mockMvc.perform(post(BASE + "/restaurantes")
                            .header("Authorization", bearer(expired))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void malformedTokenShouldBeRejected() throws Exception {
            mockMvc.perform(post(BASE + "/restaurantes")
                            .header("Authorization", "Bearer not.a.valid.jwt")
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void wrongSecretTokenShouldBeRejected() throws Exception {
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                    "another_secret_key_that_is_at_least_32_chars!".getBytes(StandardCharsets.UTF_8));
            String wrongToken = Jwts.builder()
                    .setSubject("1")
                    .claim("correo", "a@test.com")
                    .claim("rol", "ADMINISTRADOR")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(wrongKey, SignatureAlgorithm.HS256)
                    .compact();
            mockMvc.perform(post(BASE + "/restaurantes")
                            .header("Authorization", bearer(wrongToken))
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ====================================================================
    // Swagger/OpenAPI debe ser público
    // ====================================================================

    @Nested
    class PublicEndpointsTests {

        @Test
        void swaggerUiShouldBePublic() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(result -> {
                        int s = result.getResponse().getStatus();
                        org.junit.jupiter.api.Assertions.assertTrue(
                                s == 200 || s == 302,
                                "Swagger deberia ser publico, pero recibio status " + s);
                    });
        }

        @Test
        void apiDocsShouldBePublic() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }

    // ====================================================================
    // Helpers
    // ====================================================================

    /**
     * Verifica que la peticion NO fue rechazada por Spring Security (role/auth).
     * Si el status es 403 pero el body contiene "message" (de ControllerAdvisor),
     * significa que la seguridad paso y fue una excepcion de dominio (AccessDeniedException).
     */
    private void assertNotForbidden(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        ResultActions result = mockMvc.perform(req);
        int status = result.andReturn().getResponse().getStatus();
        if (status == 403) {
            String body = result.andReturn().getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertTrue(
                    body.contains("\"message\""),
                    "El endpoint rechazó con 403 por seguridad (no por lógica de negocio). Body: " + body);
        }
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String tokenForRole(String rol) {
        return switch (rol) {
            case "ADMINISTRADOR" -> adminToken;
            case "PROPIETARIO" -> propietarioToken;
            case "EMPLEADO" -> empleadoToken;
            case "CLIENTE" -> clienteToken;
            default -> throw new IllegalArgumentException("Rol desconocido: " + rol);
        };
    }

    private String generateToken(Long id, String correo, String rol) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(id.toString())
                .claim("correo", correo)
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateExpiredToken(Long id, String correo, String rol) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(id.toString())
                .claim("correo", correo)
                .claim("rol", rol)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
