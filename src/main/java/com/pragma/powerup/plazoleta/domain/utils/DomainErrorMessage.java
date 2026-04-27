package com.pragma.powerup.plazoleta.domain.utils;

public enum DomainErrorMessage {

    // Restaurante
    NIT_NOT_NUMERIC("El NIT debe ser numerico"),
    PHONE_INVALID("Telefono invalido"),
    RESTAURANT_NAME_INVALID("Nombre de restaurante invalido"),
    RESTAURANT_NOT_FOUND("Restaurante no existe"),
    OWNER_NOT_PROPIETARIO("El idPropietario no corresponde a un usuario con rol PROPIETARIO"),
    NOT_RESTAURANT_OWNER_CREATE("No puedes crear platos en restaurantes de otro propietario"),
    NOT_RESTAURANT_OWNER_MODIFY("No puedes modificar platos de otro restaurante"),
    NOT_RESTAURANT_OWNER_ASSIGN("No puedes asignar empleados en restaurantes de otro propietario"),

    // Plato
    DISH_NOT_FOUND("Plato no existe"),
    PRICE_NOT_POSITIVE("El precio debe ser mayor a 0"),
    DISH_INACTIVE("No se pueden pedir platos inactivos"),
    DISH_DIFFERENT_RESTAURANT("Un pedido debe incluir platos de un solo restaurante"),

    // Empleado
    EMPLOYEE_NOT_EMPLEADO("El idEmpleado no corresponde a un usuario con rol EMPLEADO"),
    EMPLOYEE_ALREADY_ASSIGNED_RESTAURANT("El empleado ya esta asignado a este restaurante"),
    EMPLOYEE_NOT_IN_RESTAURANT("El empleado no esta asignado a un restaurante"),
    EMPLOYEE_NOT_IN_SAME_RESTAURANT("Solo puedes tomar pedidos de tu restaurante"),

    // Pedido
    ORDER_NOT_FOUND("Pedido no existe"),
    ACTIVE_ORDER_EXISTS("Ya tienes un pedido en proceso"),
    ORDER_WITHOUT_ITEMS("El pedido debe contener platos"),
    ORDER_ALREADY_ASSIGNED("El pedido ya fue asignado o no esta pendiente"),
    ORDER_NOT_CANCELABLE("Lo sentimos, tu pedido ya esta en preparacion y no puede cancelarse"),
    ORDER_NOT_IN_PREPARACION("Solo pedidos EN_PREPARACION pueden pasar a LISTO"),
    ORDER_NOT_LISTO("Solo pedidos LISTO pueden pasar a ENTREGADO"),
    NOT_ORDER_CLIENT("No puedes cancelar pedidos de otro cliente"),
    NOT_ASSIGNED_EMPLOYEE_READY("Solo el empleado asignado puede marcar LISTO"),
    NOT_ASSIGNED_EMPLOYEE_DELIVER("Solo el empleado asignado puede entregar"),

    // PIN
    PIN_INVALID("PIN invalido"),
    PIN_GENERATION_FAILED("No se pudo generar un PIN unico");

    private final String message;

    DomainErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
