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

## Pruebas y cobertura (JUnit 5 + JaCoCo)

Todas las pruebas están escritas en **JUnit 5 (Jupiter)** y la cobertura se mide con
**JaCoCo 0.8.9** ya configurado en `build.gradle` (plugin `jacoco`, exclusiones para
DTOs/entidades JPA/clase main/MapStruct generado y reglas de verificación).

### Tareas Gradle disponibles

| Tarea | Qué hace |
|---|---|
| `./gradlew test` | Ejecuta los tests JUnit 5 (`useJUnitPlatform`) y dispara `jacocoTestReport` al finalizar. |
| `./gradlew jacocoTestReport` | Genera los reportes HTML, XML y CSV bajo `build/reports/jacoco/`. |
| `./gradlew jacocoTestCoverageVerification` | Valida los umbrales mínimos de cobertura. |
| `./gradlew check` | Ejecuta tests + reporte + verificación de cobertura. |

### Generar el reporte

```bash
# Linux / macOS
./gradlew clean test jacocoTestReport

# Windows (PowerShell o CMD)
.\gradlew.bat clean test jacocoTestReport
```

> Nota: si Gradle no logra resolver dependencias por restricciones de red, agrega
> `--offline` para usar el caché local: `./gradlew test jacocoTestReport --offline`.

### Visualizar el reporte

Los archivos quedan en:

- **HTML (recomendado)**: `build/reports/jacoco/html/index.html`
- **XML (CI / SonarQube)**: `build/reports/jacoco/jacoco.xml`
- **CSV**: `build/reports/jacoco/jacoco.csv`

Abrir el HTML:

```bash
# Windows
start build\reports\jacoco\html\index.html

# macOS
open build/reports/jacoco/html/index.html

# Linux
xdg-open build/reports/jacoco/html/index.html
```

### Reglas de cobertura activas

Definidas en `jacocoTestCoverageVerification` (build.gradle):

| Ámbito | Métrica | Umbral mínimo |
|---|---|---|
| Bundle (global) | INSTRUCTION | 75% |
| Bundle (global) | BRANCH | 65% |
| `domain.usecase` | INSTRUCTION | 90% |
| `domain.usecase` | BRANCH | 85% |

`check.dependsOn jacocoTestCoverageVerification`, por lo que el build romperá si
una regla no se cumple.

### Cobertura actual del microservicio

Última ejecución (`./gradlew clean test jacocoTestReport`):

| Métrica | Cubierto / Total | % |
|---|---|---|
| Instrucciones | 1965 / 2576 | **76,28%** |
| Ramas | 78 / 108 | **72,22%** |
| Líneas | 452 / 557 | **81,15%** |
| Métodos | 106 / 146 | **72,60%** |
| Clases | 34 / 34 | **100%** |

Cobertura por paquete (instrucciones):

| Paquete | % |
|---|---|
| `domain.model` | 100% |
| `domain.utils` | 100% |
| `domain.usecase` (núcleo hexagonal) | **96,49%** |
| `application.mapper` | 100% |
| `application.handler.impl` | 67,48% |
| `infrastructure.out.http.adapter` | 92,31% |
| `infrastructure.out.http.client` | 94,98% |
| `infrastructure.input.rest` | 29,45% |
| `infrastructure.out.jpa.adapter` | 33,05% |
| `infrastructure.out.pin` | 38,10% |

> **Cumplimiento:** Ni `RETO.MD`, `listado_HU.md` ni `plan.md` definen un umbral
> de cobertura específico para el reto (Fase 6 del plan habla de pruebas
> "orientativas"). Las reglas configuradas siguen una práctica de industria
> exigiendo cobertura alta en el dominio (núcleo de negocio) y un mínimo
> razonable en el resto. El núcleo (`domain.usecase`) supera ampliamente el
> umbral del 90% (96,49%); el global (76,28%) cumple el 75% del bundle.

### Cómo elevar la cobertura global

Las áreas de menor cobertura están en infraestructura por diseño (su
contrato real se valida vía pruebas de integración):

- `infrastructure.input.rest` se cubre indirectamente con `SecurityAuthorizationTest`,
  `ControllerContractBaselineTest` y `PlazoletaHandlerWiringTest`. Para subirlo,
  añadir tests `@WebMvcTest` por endpoint feliz/error.
- `infrastructure.out.jpa.adapter` se eleva con tests `@DataJpaTest` contra H2
  (ya disponible en `testRuntimeOnly`).
- `infrastructure.out.pin` se eleva con un test directo del `RandomPinGeneratorAdapter`.
