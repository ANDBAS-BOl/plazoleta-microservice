package com.pragma.powerup.plazoleta.infrastructure.out.http.client;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Component
public class RequestAuthHeaderProvider implements AuthHeaderProvider {

    @Override
    public Optional<String> getAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        HttpServletRequest request = attributes.getRequest();
        return Optional.ofNullable(request.getHeader("Authorization"));
    }
}
