package com.pragma.powerup.plazoleta.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;

    // El secreto se comparte con usuarios-microservice y se usa tal cual como material
    // de clave HMAC (bytes UTF-8), sin derivacion adicional: ambos servicios deben
    // partir del mismo texto. HS256 exige minimo 32 bytes.
    public JwtTokenProvider(@Value("${security.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public UsuarioPrincipal getPrincipalFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
        Long id = Long.parseLong(claims.getSubject());
        String correo = claims.get("correo", String.class);
        Rol rol = Rol.valueOf(claims.get("rol", String.class));
        return new UsuarioPrincipal(id, correo, rol);
    }
}
