package com.pragma.powerup.plazoleta.infrastructure.exceptionhandler;

import com.pragma.powerup.plazoleta.domain.exception.AccessDeniedException;
import com.pragma.powerup.plazoleta.domain.exception.BusinessRuleException;
import com.pragma.powerup.plazoleta.domain.exception.DomainException;
import com.pragma.powerup.plazoleta.domain.exception.InternalProcessException;
import com.pragma.powerup.plazoleta.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ControllerAdvisorTest {

    private final ControllerAdvisor advisor = new ControllerAdvisor();

    @Test
    void handleBusinessRule_returns400WithMessage() {
        ResponseEntity<Map<String, String>> response =
                advisor.handleBusinessRule(new BusinessRuleException("regla"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("regla", response.getBody().get("message"));
    }

    @Test
    void handleNotFound_returns404WithMessage() {
        ResponseEntity<Map<String, String>> response =
                advisor.handleNotFound(new ResourceNotFoundException("missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("missing", response.getBody().get("message"));
    }

    @Test
    void handleAccessDenied_returns403WithMessage() {
        ResponseEntity<Map<String, String>> response =
                advisor.handleAccessDenied(new AccessDeniedException("denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("denied", response.getBody().get("message"));
    }

    @Test
    void handleInternalProcess_returns500WithMessage() {
        ResponseEntity<Map<String, String>> response =
                advisor.handleInternalProcess(new InternalProcessException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("boom", response.getBody().get("message"));
    }

    @Test
    void handleDomainException_returns400WithMessage() {
        ResponseEntity<Map<String, String>> response =
                advisor.handleDomainException(new TestDomainException("dom"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("dom", response.getBody().get("message"));
    }

    @Test
    void handleDataIntegrity_returns409WithMessage() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException("duplicate");

        ResponseEntity<Map<String, String>> response = advisor.handleDataIntegrity(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody().get("message"));
    }

    @Test
    void handleNotReadable_returns400WithGenericMessage() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<Map<String, String>> response = advisor.handleNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().get("message"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleValidation_returns400WithFieldErrors() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "nombre", "no debe estar en blanco"));
        bindingResult.addError(new FieldError("request", "idPropietario", "no debe ser nulo"));

        Method dummy = DummyController.class.getDeclaredMethod("dummy", String.class);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(new org.springframework.core.MethodParameter(dummy, 0), bindingResult);

        ResponseEntity<Map<String, Object>> response = advisor.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().get("message"));
        Object errors = response.getBody().get("errors");
        assertInstanceOf(Map.class, errors);
        Map<String, String> errorMap = (Map<String, String>) errors;
        assertTrue(errorMap.containsKey("nombre"));
        assertTrue(errorMap.containsKey("idPropietario"));
        assertEquals("no debe estar en blanco", errorMap.get("nombre"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void handleConstraintViolation_returns400WithViolations() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        javax.validation.Path path = mock(javax.validation.Path.class);
        when(path.toString()).thenReturn("idEmpleado");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("no debe ser nulo");

        Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();
        violations.add(violation);
        ConstraintViolationException ex = new ConstraintViolationException(violations);

        ResponseEntity<Map<String, Object>> response = advisor.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> errorMap = (Map<String, String>) response.getBody().get("errors");
        assertEquals("no debe ser nulo", errorMap.get("idEmpleado"));
    }

    private static class TestDomainException extends DomainException {
        TestDomainException(String message) {
            super(message);
        }
    }

    private static class DummyController {
        @SuppressWarnings("unused")
        public void dummy(String body) {
        }
    }
}
