# PulsePass 🎟️

PulsePass es un sistema backend robusto desarrollado en Java con Spring Boot para la gestión integral de eventos, recintos, artistas y la comercialización de entradas (tickets). Este proyecto forma parte de un desarrollo académico enfocado en buenas prácticas de persistencia con Spring Data JPA, control de esquemas mediante migraciones y pruebas automatizadas rigurosas.

## Tecnologías y Herramientas

* **Java 17+ / Spring Boot** (Framework principal)
* **Spring Data JPA / Hibernate** (Capa de persistencia)
* **PostgreSQL** (Base de datos relacional)
* **Flyway** (Control de versiones y migraciones de base de datos)
* **Maven** (Gestor de dependencias y construcción)
* **JUnit 5 / Spring Boot Test** (Suite de pruebas de integración)

## 📂 Arquitectura y Estructura del Proyecto

El proyecto sigue una arquitectura en capas tradicional orientada al dominio:

```text
src/
├── main/
│   ├── java/edu/unimagdalena/pulsepass/
│   │   ├── domain/       # Entidades JPA (User, Event, Venue, Ticket, etc.)
│   │   └── repository/   # Repositorios Spring Data JPA con consultas JPQL avanzadas
│   └── resources/
│       ├── db/migration/ # Scripts de migración SQL de Flyway
│       └── application.yaml # Configuración de entorno y base de datos
└── test/
    └── java/edu/unimagdalena/pulsepass/
        ├── repository/ # Pruebas de integración y persistencia (*IT.java)
```


# ⚙️ Configuración y Requisitos Previos
Antes de ejecutar el proyecto, asegúrate de contar con lo siguiente instalado en tu equipo:

JDK 17 o superior.

Maven (o utilizar el wrapper ./mvnw).

Una instancia de PostgreSQL corriendo localmente.

# 1. Configuración de la Base de Datos
Modifica el archivo src/main/resources/application.yaml con tus credenciales locales de PostgreSQL:

```text
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pulsepass_db
    username: tu_usuario
    password: tu_contraseña
  jpa:
    hibernate:
      ddl-auto: validate # Hibernate valida el esquema sin modificarlo (gestionado por Flyway)
    show-sql: true
  flyway:
    enabled: true
```

#🧪 Ejecución de Pruebas
Para garantizar la integridad del modelo de datos, las restricciones UNIQUE, CHECK y las consultas personalizadas JPQL, el proyecto cuenta con una suite completa de pruebas de integración.

Ejecuta todas las pruebas con Maven:

Bash
```text
mvn clean test
```

#📄 Migraciones de Base de Datos (Flyway)
El control del esquema se maneja estrictamente mediante scripts versionados ubicados en src/main/resources/db/migration/, asegurando consistencia entre los entornos de desarrollo y producción.
