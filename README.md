# Plazoleta Microservice

Es el microservicio principal de la lógica de negocio para gestionar el flujo de pedidos y el catálogo (restaurantes y platos) del Sistema Plaza de Comidas.

## Rol en el Sistema
* **Catálogo:** Administración de Restaurantes y Platos (creación, modificación, borrado lógico). Restricción: Cada pedido contiene platos de un solo restaurante.
* **Flujo de pedidos:** Gestiona los estados del pedido: PENDIENTE -> EN_PREPARACION -> LISTO -> ENTREGADO (y CANCELADO desde PENDIENTE).
* **PIN de Seguridad:** Genera un PIN numérico único de 6 dígitos al pasar al estado "LISTO" y lo consume/invalida en "ENTREGADO". Envía solicitudes a Mensajería para el SMS.
* **Asignación de Empleados:** Asigna el primer empleado que acepta un pedido PENDIENTE.
* **Autenticación:** Valida de forma autónoma la firma del token JWT utilizando la clave secreta compartida con Usuarios.
* **Base de Datos:** MySQL.

## Requisitos Previos
* JDK 17 o superior (Recomendado JDK 21 compilando a Target 17).
* Gradle 8.5.
* Docker y Docker Compose para levantar la base de datos (puerto 3306).

## Cómo ejecutar localmente
Repositorio de infraestructura: [plazoleta-deployment](https://github.com/ANDBAS-BOl/plazoleta-deployment)

1. Levantar bases de datos:
   Desde la carpeta `plazoleta-deployment`, ejecute:
   ```bash
   docker compose -f docker/compose-db.yml up -d
   ```
2. Iniciar el microservicio:
   Desde la carpeta `plazoleta-microservice`, ejecute:
   ```bash
   ./gradlew bootRun
   ```

El servicio se iniciará por defecto en el puerto `8082`.
