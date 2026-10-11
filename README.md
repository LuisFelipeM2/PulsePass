# PulsePass 🎟️

PulsePass es un sistema backend robusto desarrollado en Java con Spring Boot para la gestión integral de eventos, recintos, artistas y la comercialización de entradas (tickets). Este proyecto forma parte de un desarrollo académico enfocado en buenas prácticas de persistencia con Spring Data JPA, control de esquemas mediante migraciones, una capa de servicios con reglas de negocio y pruebas automatizadas rigurosas.

## Tecnologías y Herramientas

* **Java 21 / Spring Boot 4** (Framework principal)
* **Spring Data JPA / Hibernate** (Capa de persistencia)
* **PostgreSQL** (Base de datos relacional)
* **Flyway** (Control de versiones y migraciones de base de datos)
* **MapStruct** (Conversión de entidades a DTOs)
* **Lombok** (Reducción de código repetitivo en entidades)
* **Maven** (Gestor de dependencias y construcción)
* **JUnit Jupiter / Mockito / AssertJ** (Pruebas unitarias de la capa de servicios)
* **Spring Boot Test / Testcontainers** (Pruebas de integración de persistencia)

## 📂 Arquitectura y Estructura del Proyecto

El proyecto sigue una arquitectura en capas tradicional orientada al dominio:

```text
Controller (futuro) → Service → Repository → Entity → PostgreSQL
                        │
                        ├── DTOs (request / response)
                        └── Mappers (MapStruct)
```

```text
src/
├── main/
│   ├── java/edu/unimagdalena/PulsePass/
│   │   ├── domain/       # Entidades JPA y enums (User, Event, Venue, Ticket, etc.)
│   │   ├── repository/   # Repositorios Spring Data JPA con consultas JPQL avanzadas
│   │   ├── dto/
│   │   │   ├── request/  # DTOs de entrada (records)
│   │   │   └── response/ # DTOs de salida (records)
│   │   ├── mapper/       # Mappers MapStruct (entidad → DTO)
│   │   ├── exception/    # Excepciones de dominio
│   │   └── service/      # Interfaces de los servicios con las implementaciones de las reglas de negocio
│   │      
│   └── resources/
│       ├── db/migration/ # Scripts de migración SQL de Flyway
│       └── application.yaml # Configuración de entorno y base de datos
└── test/
    └── java/edu/unimagdalena/PulsePass/
        ├── repository/   # Pruebas de integración y persistencia (*IT.java)
        └── service/ # Pruebas unitarias de los servicios (Mockito)
```

## 🧩 Capa de Servicios

La capa de servicios es la frontera entre las futuras capas de exposición (controllers) y el modelo persistente. Aplica las reglas de negocio, coordina los repositories, controla las transacciones y retorna DTOs sin exponer entidades JPA.

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

#  🧪 Ejecución de Pruebas
El proyecto cuenta con dos tipos de pruebas:

* **Pruebas unitarias de servicios** (`src/test/.../service/impl`): usan JUnit Jupiter, Mockito y AssertJ con repositories y mappers simulados. No levantan Spring ni PostgreSQL, por lo que no requieren Docker.
* **Pruebas de integración de persistencia** (`src/test/.../repository`, clases `*IT`): validan el modelo de datos, las restricciones UNIQUE y CHECK y las consultas personalizadas JPQL sobre un PostgreSQL levantado con Testcontainers. Requieren Docker en ejecución.

Ejecuta todas las pruebas con Maven:

```bash
mvn clean test
```

Para ejecutar solo las pruebas unitarias de los servicios:

```bash
mvn test "-Dtest=*ServiceImplTest"
```

Para comprobar únicamente que el código compila (sin ejecutar pruebas ni requerir Docker):

```bash
mvn clean test-compile
```

# 📄 Migraciones de Base de Datos (Flyway)
El control del esquema se maneja estrictamente mediante scripts versionados ubicados en `src/main/resources/db/migration/`, asegurando consistencia entre los entornos de desarrollo y producción.

## 👥 Autores

* **Julio Cesar Carrillo Correa** - *Desarrollo Backend / Capa de Persistencia y Servicios*
* **Luis Felipe Muñoz Camacho** - *Desarrollo Backend / Capa de Persistencia y Servicios*
* Universidad del Magdalena
