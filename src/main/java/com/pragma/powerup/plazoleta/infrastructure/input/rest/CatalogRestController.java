package com.pragma.powerup.plazoleta.infrastructure.input.rest;

import com.pragma.powerup.plazoleta.application.dto.request.AssignEmployeeRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.request.CreateRestaurantRequest;
import com.pragma.powerup.plazoleta.application.dto.request.UpdateDishRequest;
import com.pragma.powerup.plazoleta.application.dto.response.DishResponse;
import com.pragma.powerup.plazoleta.application.dto.response.RestaurantCardResponse;
import com.pragma.powerup.plazoleta.application.handler.ICatalogHandler;
import com.pragma.powerup.plazoleta.domain.model.PageResult;
import com.pragma.powerup.plazoleta.infrastructure.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plazoleta")
@RequiredArgsConstructor
public class CatalogRestController {

    private final ICatalogHandler catalogHandler;

    @PostMapping("/restaurantes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createRestaurant(@Valid @RequestBody CreateRestaurantRequest request) {
        return Map.of("id", catalogHandler.createRestaurant(request));
    }

    @PostMapping("/platos")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> createDish(@Valid @RequestBody CreateDishRequest request,
                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        return Map.of("id", catalogHandler.createDish(request, principal.getId()));
    }

    @PostMapping("/restaurantes/{idRestaurante}/empleados")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> assignEmployeeToRestaurant(@PathVariable Long idRestaurante,
                                                         @Valid @RequestBody AssignEmployeeRequest request,
                                                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        return Map.of("id", catalogHandler.assignEmployeeToRestaurant(idRestaurante, request, principal.getId()));
    }

    @PatchMapping("/platos/{idPlato}")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateDish(@PathVariable Long idPlato,
                           @Valid @RequestBody UpdateDishRequest request,
                           @AuthenticationPrincipal UsuarioPrincipal principal) {
        catalogHandler.updateDish(idPlato, request, principal.getId());
    }

    @PatchMapping("/platos/{idPlato}/estado")
    @PreAuthorize("hasRole('PROPIETARIO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeDishStatus(@PathVariable Long idPlato,
                                 @RequestParam boolean activo,
                                 @AuthenticationPrincipal UsuarioPrincipal principal) {
        catalogHandler.changeDishStatus(idPlato, activo, principal.getId());
    }

    @GetMapping("/restaurantes")
    @PreAuthorize("hasRole('CLIENTE')")
    public Page<RestaurantCardResponse> listRestaurants(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        PageResult<RestaurantCardResponse> result = catalogHandler.listRestaurants(page, size);
        return toSpringPage(result, page, size);
    }

    @GetMapping("/restaurantes/{idRestaurante}/platos")
    @PreAuthorize("hasRole('CLIENTE')")
    public Page<DishResponse> listDishes(@PathVariable Long idRestaurante,
                                         @RequestParam(required = false) String categoria,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        PageResult<DishResponse> result = catalogHandler.listDishes(idRestaurante, categoria, page, size);
        return toSpringPage(result, page, size);
    }

    private <T> Page<T> toSpringPage(PageResult<T> result, int page, int size) {
        return new PageImpl<>(result.content(), PageRequest.of(page, size), result.totalElements());
    }
}
