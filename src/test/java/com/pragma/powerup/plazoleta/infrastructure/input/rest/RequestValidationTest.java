package com.pragma.powerup.plazoleta.infrastructure.input.rest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que cuando el cuerpo de la peticion incumple Bean Validation
 * el sistema responde 400 (Bad Request) con un payload `{message, errors}`,
 * en lugar de 500 Internal Server Error.
 *
 * Cubre el cierre del hallazgo HU2: campos obligatorios faltantes en el DTO
 * deben rechazarse con 400 y mensaje claro.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestValidationTest {

    private static final String SECRET = "test_jwt_secret_key_at_least_32_characters_long_for_hmac";
    private static final String BASE = "/api/v1/plazoleta";

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String propietarioToken;
    private String clienteToken;
    private String empleadoToken;

    @BeforeEach
    void setUp() {
        adminToken = generateToken(1L, "ADMINISTRADOR");
        propietarioToken = generateToken(2L, "PROPIETARIO");
        clienteToken = generateToken(4L, "CLIENTE");
        empleadoToken = generateToken(3L, "EMPLEADO");
    }

    // ───────────────────────────────────────────────────────────────────────
    // HU 2 – Crear restaurante: campos obligatorios
    // ───────────────────────────────────────────────────────────────────────

    @Test
    void createRestaurant_missingNombre_shouldReturn400() throws Exception {
        String body = """
                {"nit":"123456","direccion":"x","telefono":"+573001112233","urlLogo":"http://l","idPropietario":2}
                """;
        mockMvc.perform(post(BASE + "/restaurantes")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors.nombre").exists());
    }

    @Test
    void createRestaurant_blankFields_shouldReturn400WithAllErrors() throws Exception {
        String body = """
                {"nombre":"","nit":"","direccion":"","telefono":"","urlLogo":"","idPropietario":null}
                """;
        mockMvc.perform(post(BASE + "/restaurantes")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.nit").exists())
                .andExpect(jsonPath("$.errors.direccion").exists())
                .andExpect(jsonPath("$.errors.telefono").exists())
                .andExpect(jsonPath("$.errors.urlLogo").exists())
                .andExpect(jsonPath("$.errors.idPropietario").exists());
    }

    @Test
    void createRestaurant_emptyBody_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE + "/restaurantes")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void createRestaurant_malformedJson_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE + "/restaurantes")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ───────────────────────────────────────────────────────────────────────
    // HU 3 – Crear plato: campos obligatorios + precio > 0
    // ───────────────────────────────────────────────────────────────────────

    @Test
    void createDish_missingFields_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE + "/platos")
                        .header("Authorization", bearer(propietarioToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.precio").exists())
                .andExpect(jsonPath("$.errors.descripcion").exists())
                .andExpect(jsonPath("$.errors.urlImagen").exists())
                .andExpect(jsonPath("$.errors.categoria").exists())
                .andExpect(jsonPath("$.errors.idRestaurante").exists());
    }

    @Test
    void createDish_priceZero_shouldReturn400() throws Exception {
        String body = """
                {"nombre":"X","precio":0,"descripcion":"d","urlImagen":"http://x","categoria":"c","idRestaurante":1}
                """;
        mockMvc.perform(post(BASE + "/platos")
                        .header("Authorization", bearer(propietarioToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.precio").exists());
    }

    @Test
    void createDish_priceNegative_shouldReturn400() throws Exception {
        String body = """
                {"nombre":"X","precio":-5,"descripcion":"d","urlImagen":"http://x","categoria":"c","idRestaurante":1}
                """;
        mockMvc.perform(post(BASE + "/platos")
                        .header("Authorization", bearer(propietarioToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.precio").exists());
    }

    // ───────────────────────────────────────────────────────────────────────
    // HU 4 – Modificar plato
    // ───────────────────────────────────────────────────────────────────────

    @Test
    void updateDish_blankBody_shouldReturn400() throws Exception {
        mockMvc.perform(patch(BASE + "/platos/1")
                        .header("Authorization", bearer(propietarioToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.precio").exists())
                .andExpect(jsonPath("$.errors.descripcion").exists());
    }

    // ───────────────────────────────────────────────────────────────────────
    // HU 6/HU 8 – Asignar empleado: idEmpleado obligatorio
    // ───────────────────────────────────────────────────────────────────────

    @Test
    void assignEmployee_missingId_shouldReturn400() throws Exception {
        mockMvc.perform(post(BASE + "/restaurantes/1/empleados")
                        .header("Authorization", bearer(propietarioToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idEmpleado").exists());
    }

    // ───────────────────────────────────────────────────────────────────────
    // HU 11 – Crear pedido: lista de platos no vacia
    // ───────────────────────────────────────────────────────────────────────

    @Test
    void createOrder_emptyPlatos_shouldReturn400() throws Exception {
        String body = """
                {"idRestaurante":1,"telefonoCliente":"x","platos":[]}
                """;
        mockMvc.perform(post(BASE + "/pedidos")
                        .header("Authorization", bearer(clienteToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.platos").exists());
    }

    @Test
    void createOrder_missingIdRestaurante_shouldReturn400() throws Exception {
        String body = """
                {"telefonoCliente":"x","platos":[{"idPlato":1,"cantidad":1}]}
                """;
        mockMvc.perform(post(BASE + "/pedidos")
                        .header("Authorization", bearer(clienteToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idRestaurante").exists());
    }

    @Test
    void createOrder_quantityZero_shouldReturn400() throws Exception {
        String body = """
                {"idRestaurante":1,"telefonoCliente":"x","platos":[{"idPlato":1,"cantidad":0}]}
                """;
        mockMvc.perform(post(BASE + "/pedidos")
                        .header("Authorization", bearer(clienteToken))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    // ───────────────────────────────────────────────────────────────────────
    // HU 15 – Entregar pedido: PIN exacto de 6 digitos
    // ───────────────────────────────────────────────────────────────────────

    @Test
    void deliverOrder_blankPin_shouldReturn400() throws Exception {
        mockMvc.perform(patch(BASE + "/pedidos/1/entregar")
                        .header("Authorization", bearer(empleadoToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pin").exists());
    }

    @Test
    void deliverOrder_pinNot6Digits_shouldReturn400() throws Exception {
        mockMvc.perform(patch(BASE + "/pedidos/1/entregar")
                        .header("Authorization", bearer(empleadoToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pin\":\"12abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pin").exists());
    }

    // ───────────────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────────────

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String generateToken(Long id, String rol) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(id.toString())
                .claim("correo", "user@test.com")
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
