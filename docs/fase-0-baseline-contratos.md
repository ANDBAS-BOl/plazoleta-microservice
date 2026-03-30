# Fase 0 - Baseline de contratos y no regresion

Este documento congela el comportamiento actual de `plazoleta-microservice` antes de migrar arquitectura.
El objetivo es detectar cualquier drift funcional durante las fases de refactor.

## Endpoints actuales de `PlazoletaController`

Base path: `/api/v1/plazoleta`

| Metodo | Path | Rol | Status esperado | Respuesta |
| --- | --- | --- | --- | --- |
| `POST` | `/restaurantes` | `ADMINISTRADOR` | `201` | `{ "id": <long> }` |
| `POST` | `/platos` | `PROPIETARIO` | `201` | `{ "id": <long> }` |
| `POST` | `/restaurantes/{idRestaurante}/empleados` | `PROPIETARIO` | `201` | `{ "id": <long> }` |
| `PATCH` | `/platos/{idPlato}` | `PROPIETARIO` | `204` | sin cuerpo |
| `PATCH` | `/platos/{idPlato}/estado?activo={bool}` | `PROPIETARIO` | `204` | sin cuerpo |
| `GET` | `/restaurantes?page={n}&size={n}` | `CLIENTE` | `200` | `Page<RestaurantCardResponse>` |
| `GET` | `/restaurantes/{idRestaurante}/platos?categoria={opt}&page={n}&size={n}` | `CLIENTE` | `200` | `Page<DishResponse>` |
| `POST` | `/pedidos` | `CLIENTE` | `201` | `OrderResponse` |
| `GET` | `/pedidos?estado={ESTADO}&page={n}&size={n}` | `EMPLEADO` | `200` | `Page<OrderResponse>` |
| `PATCH` | `/pedidos/{idPedido}/asignar` | `EMPLEADO` | `200` | `OrderResponse` |
| `PATCH` | `/pedidos/{idPedido}/listo` | `EMPLEADO` | `200` | `OrderResponse` |
| `PATCH` | `/pedidos/{idPedido}/entregar` | `EMPLEADO` | `200` | `OrderResponse` |
| `PATCH` | `/pedidos/{idPedido}/cancelar` | `CLIENTE` | `200` | `OrderResponse` |
| `GET` | `/pedidos/{idPedido}/trazabilidad` | `CLIENTE` | `200` | `Object` (proxy trazabilidad) |
| `GET` | `/pedidos/eficiencia` | `PROPIETARIO` | `200` | `EficienciaResponse` |

## Contratos externos actualmente consumidos por Plazoleta

### Usuarios

- `GET /api/v1/usuarios/{idUsuario}/validacion-propietario`
  - Header: `Authorization: Bearer <token>`
  - Uso: validacion HU2 al crear restaurante.
  - Respuesta esperada: JSON con `propietarioValido`.
- `GET /api/v1/usuarios/{idUsuario}/validacion-empleado`
  - Header: `Authorization: Bearer <token>`
  - Uso: validacion HU6/HU13 al asignar empleado a restaurante.
  - Respuesta esperada: JSON con `empleadoValido`.

### Mensajeria

- `POST /api/v1/mensajeria/sms`
  - Header: `Authorization` (si esta disponible en contexto)
  - Body:
    - `phoneNumber: string`
    - `message: string`
  - Uso: HU14 al pasar pedido a `LISTO`.

### Trazabilidad

- `POST /api/v1/trazabilidad/eventos`
  - Header: `Authorization` (si esta disponible en contexto)
  - Body minimo:
    - `idPedido`
    - `idCliente`
    - `idRestaurante`
    - `idEmpleado`
    - `estadoAnterior`
    - `estadoNuevo`
  - Uso: registrar transiciones de pedido.
- `GET /api/v1/trazabilidad/pedidos/{idPedido}`
  - Header: `Authorization` (si esta disponible en contexto)
  - Uso: HU17 para consultar trazabilidad del cliente.

## Cobertura de pruebas de baseline (fase 0)

- `PlazoletaControllerContractBaselineTest` congela rutas, metodos HTTP, roles y status de respuesta del controller.
- `PlazoletaServiceContractTest` valida contratos HTTP salientes de Usuarios, Mensajeria y Trazabilidad (incluyendo consulta de trazabilidad).
