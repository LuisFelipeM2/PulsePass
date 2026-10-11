# PulsePass 🎟️

PulsePass es un sistema backend robusto desarrollado en Java con Spring Boot para la gestión integral de eventos, recintos, artistas y la comercialización de entradas (tickets). Este proyecto forma parte de un desarrollo académico enfocado en buenas prácticas de persistencia con Spring Data JPA, control de esquemas mediante migraciones, una capa de servicios con reglas de negocio, una API REST con manejo uniforme de errores y pruebas automatizadas rigurosas.

## Tecnologías y Herramientas

* **Java 21 / Spring Boot 4** (Framework principal)
* **Spring MVC** (API REST: controllers y manejo global de excepciones)
* **Bean Validation** (Validación de los requests HTTP)
* **Spring Data JPA / Hibernate** (Capa de persistencia)
* **PostgreSQL** (Base de datos relacional)
* **Flyway** (Control de versiones y migraciones de base de datos)
* **MapStruct** (Conversión de entidades a DTOs)
* **Lombok** (Reducción de código repetitivo en entidades)
* **Maven** (Gestor de dependencias y construcción)
* **JUnit Jupiter / Mockito / AssertJ** (Pruebas unitarias de la capa de servicios)
* **MockMvc / `@WebMvcTest`** (Pruebas de contrato HTTP de los controllers)
* **Spring Boot Test / Testcontainers** (Pruebas de integración de persistencia)

## 📂 Arquitectura y Estructura del Proyecto

El proyecto sigue una arquitectura en capas tradicional orientada al dominio:

```text
HTTP → Controller → Service → Repository → Entity → PostgreSQL
           │           │
           │           ├── DTOs (request / response)
           │           └── Mappers (MapStruct)
           │
           └── GlobalExceptionHandler (excepciones → ErrorResponse)
```

```text
src/
├── main/
│   ├── java/edu/unimagdalena/PulsePass/
│   │   ├── controller/   # Controllers REST (Venue, Event, Artist, User, Ticket)
│   │   ├── domain/       # Entidades JPA y enums (User, Event, Venue, Ticket, etc.)
│   │   ├── repository/   # Repositorios Spring Data JPA con consultas JPQL avanzadas
│   │   ├── dto/
│   │   │   ├── request/  # DTOs de entrada (records, con Bean Validation)
│   │   │   └── response/ # DTOs de salida (records) y ErrorResponse
│   │   ├── mapper/       # Mappers MapStruct (entidad → DTO)
│   │   ├── exception/    # Excepciones de dominio y GlobalExceptionHandler
│   │   └── service/      # Interfaces de los servicios con las implementaciones de las reglas de negocio
│   │
│   └── resources/
│       ├── db/migration/ # Scripts de migración SQL de Flyway
│       └── application.yaml # Configuración de entorno y base de datos
└── test/
    └── java/edu/unimagdalena/PulsePass/
        ├── repository/   # Pruebas de integración y persistencia (*IT.java)
        ├── service/      # Pruebas unitarias de los servicios (Mockito)
        └── controller/   # Pruebas de contrato HTTP de los controllers (@WebMvcTest + MockMvc)
```

## 🧩 Capa de Servicios

La capa de servicios es la frontera entre la capa de controllers y el modelo persistente. Aplica las reglas de negocio, coordina los repositories, controla las transacciones y retorna DTOs sin exponer entidades JPA.

| Servicio | Responsabilidad |
|---|---|
| `VenueService` | Consultar lugares por código y listar los lugares activos |
| `ArtistService` | Consultar artistas por id o nombre artístico y listar los activos |
| `EventService` | Crear, consultar y publicar eventos; asociar artistas; consultar eventos por artista |
| `UserService` | Registrar usuarios junto con su perfil y consultarlos por email o username |
| `TicketService` | Comprar, consultar, cancelar y marcar tickets como usados |

### Principios de diseño

* **Sin entidades en los contratos:** los servicios reciben y retornan DTOs implementados como `record`.
* **Mapeo con MapStruct:** la conversión de entidad a DTO se hace con mappers generados en compilación.
* **Inyección por constructor** y contratos mediante interfaces.
* **Transacciones:** las escrituras usan `@Transactional` y las lecturas `@Transactional(readOnly = true)`. La compra de un ticket es atómica: si una validación falla, se revierte todo.
* **Excepciones de dominio:** `ResourceNotFoundException` (el recurso no existe), `DuplicateResourceException` (conflicto de unicidad) y `BusinessRuleException` (la operación no cumple una regla de negocio).
* **Reglas trazables:** en el código, las reglas de negocio llevan comentarios con el identificador del documento de requisitos (por ejemplo, `BR-EVENT-001`).

### Reglas destacadas

* Un evento nuevo siempre inicia en `DRAFT` y solo puede publicarse desde ese estado, con fecha futura y recinto activo.
* Para comprar un ticket, el usuario debe existir y estar activo, el evento debe estar `PUBLISHED` con fecha futura, el usuario debe cumplir la edad mínima (calculada en la fecha del evento) y debe haber capacidad disponible en el recinto.
* Si una compra completa la capacidad del recinto, el evento pasa a `SOLD_OUT` en la misma transacción.
* El precio del ticket lo calcula el sistema según su tipo (`BigDecimal`); el cliente no lo envía.

### Ciclos de estado

```text
Evento:  DRAFT → PUBLISHED → SOLD_OUT → FINISHED
         DRAFT → CANCELLED   |   PUBLISHED → CANCELLED

Ticket:  PAID → USED
         PAID → CANCELLED
```

## 🌐 Capa de Controladores (API REST)

La capa de controllers expone por HTTP los 20 métodos de los servicios. Es una capa delgada: traduce la petición HTTP a una llamada al Service y devuelve la respuesta como DTO. No contiene reglas de negocio, no accede a repositories y no retorna entidades JPA.

| Controller | Servicio que consume | Endpoints |
|---|---|---|
| `VenueController` | `VenueService` | 2 |
| `EventController` | `EventService` | 6 |
| `ArtistController` | `ArtistService` | 3 |
| `UserController` | `UserService` | 3 |
| `TicketController` | `TicketService` | 6 |

### Endpoints principales

| Método | Endpoint | Operación | Respuesta exitosa |
|---|---|---|---|
| GET | `/api/venues/{code}` | Consultar venue por código | 200 |
| GET | `/api/venues/active` | Listar venues activos | 200 |
| POST | `/api/events` | Crear evento | 201 |
| GET | `/api/events/{eventCode}` | Consultar evento por código | 200 |
| GET | `/api/events/published` | Listar eventos publicados | 200 |
| PATCH | `/api/events/{eventCode}/publish` | Publicar evento | 200 |
| POST | `/api/events/{eventCode}/artists/{artistId}` | Asociar artista a un evento | 200 |
| GET | `/api/events/by-artist?stageName=...` | Buscar eventos por artista | 200 |
| GET | `/api/artists/{id}` | Consultar artista por id | 200 |
| GET | `/api/artists/by-stage-name?stageName=...` | Consultar artista por nombre artístico | 200 |
| GET | `/api/artists/active` | Listar artistas activos | 200 |
| POST | `/api/users` | Registrar usuario | 201 |
| GET | `/api/users/by-email?email=...` | Consultar usuario por email | 200 |
| GET | `/api/users/by-username?username=...` | Consultar usuario por username | 200 |
| POST | `/api/tickets` | Comprar ticket | 201 |
| GET | `/api/tickets/{ticketCode}` | Consultar ticket por código | 200 |
| GET | `/api/tickets/by-user?email=...` | Consultar tickets de un usuario | 200 |
| GET | `/api/events/{eventCode}/tickets/paid` | Consultar tickets pagados de un evento | 200 |
| PATCH | `/api/tickets/{ticketCode}/cancel` | Cancelar ticket | 200 |
| PATCH | `/api/tickets/{ticketCode}/use` | Marcar ticket como usado | 200 |

> El endpoint `GET /api/events/{eventCode}/tickets/paid` vive en `TicketController` porque consume `TicketService`, aunque su ruta empiece por `/api/events`.

### Ejemplo: comprar un ticket

```http
POST /api/tickets
Content-Type: application/json

{
  "userEmail": "andrea@email.com",
  "eventCode": "CMF-2026",
  "type": "VIP"
}
```

El precio no se envía: lo calcula el sistema según el tipo de ticket. Respuesta `201 Created` con un `TicketResponse`.

### Validación de entrada

Los requests (`CreateEventRequest`, `RegisterUserRequest` y `PurchaseTicketRequest`) se validan con Bean Validation (`@Valid`) antes de llegar al Service: campos obligatorios, formato de email, longitud máxima y valores mínimos. Un request inválido responde `400` y nunca invoca al Service. Las reglas de negocio (usuario activo, edad mínima, capacidad, transiciones de estado) siguen viviendo en la capa de servicios.

### Códigos HTTP y manejo de errores

Todos los errores se convierten en un `ResponseEntity<ErrorResponse>` desde un único `GlobalExceptionHandler` (`@RestControllerAdvice`), sin `try/catch` en los controllers.

| Situación | Excepción | Código |
|---|---|---|
| Request inválido (Bean Validation) | `MethodArgumentNotValidException` | 400 |
| JSON mal formado o enum inválido | `HttpMessageNotReadableException` | 400 |
| Parámetro con tipo inválido | `MethodArgumentTypeMismatchException` | 400 |
| Recurso inexistente | `ResourceNotFoundException` | 404 |
| Duplicado (username o email) | `DuplicateResourceException` | 409 |
| Regla de negocio incumplida | `BusinessRuleException` | 409 |
| Error inesperado | `Exception` | 500 |

Formato uniforme de error (el campo `details` solo trae datos en errores de validación):

```json
{
  "timestamp": "2026-10-10T20:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "details": {
    "eventCode": "Event code is required",
    "venueCode": "Venue code is required"
  }
}
```

### Alcance de esta versión

La API no incluye autenticación ni autorización, paginación, versionado de rutas (`/api/v1`) ni documentación OpenAPI. Todos los endpoints son públicos.

# ⚙️ Configuración y Requisitos Previos
Antes de ejecutar el proyecto, asegúrate de contar con lo siguiente instalado en tu equipo:

* JDK 21.
* Maven (o utilizar el wrapper `./mvnw`).
* Una instancia de PostgreSQL corriendo localmente.
* Docker en ejecución (necesario para las pruebas de integración, que usan Testcontainers).

# 1. Configuración de la Base de Datos
El archivo `src/main/resources/application.yaml` toma la conexión de variables de entorno y, si no existen, usa valores por defecto:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/PulsePass}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate # Hibernate valida el esquema sin modificarlo (gestionado por Flyway)
    show-sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
```

Por defecto la aplicación se conecta a la base `PulsePass` en `localhost:5432` con el usuario `postgres` y la contraseña `postgres`. Si tu instalación usa otros valores, define las variables de entorno antes de ejecutar (ejemplo en PowerShell):

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/PulsePass"
$env:DB_USER="tu_usuario"
$env:DB_PASSWORD="tu_contraseña"
```

# 🚀 Ejecución de la Aplicación
Con la base de datos configurada, levanta la API con Maven:

```bash
mvn spring-boot:run
```

Por defecto queda disponible en `http://localhost:8080`. Ejemplo para consultar los eventos publicados:

```bash
curl http://localhost:8080/api/events/published
```

#  🧪 Ejecución de Pruebas
El proyecto cuenta con tres tipos de pruebas:

* **Pruebas unitarias de servicios** (`src/test/.../service/impl`): usan JUnit Jupiter, Mockito y AssertJ con repositories y mappers simulados. No levantan Spring ni PostgreSQL, por lo que no requieren Docker.
* **Pruebas de controllers** (`src/test/.../controller`): usan `@WebMvcTest`, `MockMvc` y los Services reemplazados con `@MockitoBean`. Validan el contrato HTTP (código de estado, `Content-Type`, JSON con `jsonPath`, `ErrorResponse`) y que un request inválido no llegue al Service. No requieren PostgreSQL ni Docker.
* **Pruebas de integración de persistencia** (`src/test/.../repository`, clases `*IT`): validan el modelo de datos, las restricciones UNIQUE y CHECK y las consultas personalizadas JPQL sobre un PostgreSQL levantado con Testcontainers. Requieren Docker en ejecución.

Ejecuta todas las pruebas con Maven:

```bash
mvn clean test
```

Para ejecutar solo las pruebas unitarias de los servicios:

```bash
mvn test "-Dtest=*ServiceImplTest"
```

Para ejecutar solo las pruebas de los controllers:

```bash
mvn test "-Dtest=*ControllerTest"
```

Para comprobar únicamente que el código compila (sin ejecutar pruebas ni requerir Docker):

```bash
mvn clean test-compile
```

# 📄 Migraciones de Base de Datos (Flyway)
El control del esquema se maneja estrictamente mediante scripts versionados ubicados en `src/main/resources/db/migration/`, asegurando consistencia entre los entornos de desarrollo y producción.

## 👥 Autores

* **Julio Cesar Carrillo Correa** - *Desarrollo Backend / Capa de Persistencia, Servicios y Controladores*
* **Luis Felipe Muñoz Camacho** - *Desarrollo Backend / Capa de Persistencia, Servicios y Controladores*
* Universidad del Magdalena
