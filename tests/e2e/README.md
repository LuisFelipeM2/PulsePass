# PulsePass — Fase 5: pruebas E2E manuales

Este material prepara la ejecución **manual** de la API real desde Postman. No contiene scripts, aserciones, extracción automática de tokens ni ejecución con Collection Runner. La referencia de contratos son los DTO, controladores, servicios y configuración de seguridad actuales en `src/main/java`.

## Entregables

- `postman/PulsePass-Manual-E2E.postman_collection.json`: colección Postman v2.1, agrupada por dominio, con una solicitud por cada una de las 22 rutas y descripciones de actor, preparación, pasos y comprobación manual. Repite login/registro manualmente cambiando las variables locales para cada actor.
- `postman/PulsePass-local.postman_environment.json`: plantilla de entorno; valores secretos y datos de ejecución quedan vacíos.
- `checklist-e2e-manual.md`: orden sugerido y matriz de las 22 rutas.
- `registro-ejecucion-manual.md`: bitácora que se diligencia a mano después de cada envío.
- `fixtures/README.md`: preparación segura de los datos de prueba.

## Preparación

1. Levanta PostgreSQL de pruebas y la aplicación con el perfil/configuración E2E aprobados por el equipo. Confirma en `application.yaml` la URL de base de datos antes de arrancar; no uses producción.
2. Ejecuta las migraciones Flyway existentes. Los scripts en `fixtures/` son instrucciones/plantillas, no se aplican automáticamente.
3. Prepara venue/artista y usuarios A/B. Para ADMIN/STAFF, registra cuentas locales con Postman para almacenar sus passwords en BCrypt y luego ejecuta `fixtures/seed-privileged-roles.sql` solo en la base E2E; ver instrucciones detalladas en `fixtures/README.md`.
4. Importa ambos JSON en Postman. Duplica el environment para la corrida privada (por ejemplo `PulsePass-local-private.postman_environment.json`, ignorado por Git), selecciona esa copia y completa ahí los valores locales. Conserva sin cambios la plantilla compartible. No exportes credenciales o JWT.
5. Envía cada request individualmente con **Send**. Inspecciona status, headers, JSON y estado posterior; registra PASS/FAIL/BLOCKED en la bitácora.

## Uso manual de login

En `03 Registro, autenticación y JWT`, registra USER_A y vuelve a enviar manualmente la misma solicitud de registro cambiando los datos por USER_B. Para login, envía la solicitud con las variables de USER_A; luego cambia manualmente `identifier` y `password` en el body a los de USER_B, STAFF y ADMIN para cada envío. Después de cada respuesta copia `accessToken` a la variable local correspondiente (`userAToken`, `userBToken`, `staffToken` o `adminToken`). `GET /api/auth/me` verifica la identidad activa. No se automatiza la copia del token.

## Contrato y discrepancias detectadas

La colección conserva los 20 endpoints de negocio más login y `/api/auth/me`. No inventa rutas de administración de fixtures ni de revocación. Antes de declarar aceptada la Fase 5, coteja estas limitaciones presentes en el código:

- `SecurityConfig` protege creación/publicación/asociación de eventos para ADMIN, compra para USER y uso/listado pagado para STAFF/ADMIN; sin embargo, `EventController.create/publish/addArtist` no hacen comprobación de principal por método. El matcher HTTP sí protege esas rutas hoy.
- `GET /api/events/{eventCode}` y `GET /api/events/by-artist` son públicos según el matcher y el controlador oculta borradores al visitante; el resultado de un borrador como visitante es 404.
- `SecurityConfig` requiere autenticación en otras rutas por `.anyRequest().authenticated()`. La implementación actual oculta usuario/ticket ajeno con 404 desde `AuthorizationService`; compra con email ajeno y rol insuficiente responden 403. El environment fija `foreignResourceStatus=404` para la política anti-enumeración de recursos ajenos.
- `GET /api/events/{eventCode}/tickets/paid` no recibe principal en el controller, pero su ruta está protegida por matcher HTTP.
- Verifica en los DTO reales los nombres/campos de respuesta. Los cuerpos de ejemplo de la colección solo usan nombres confirmados en los records de request; no son ejemplos completos de response.
- La colección de este repositorio ya lista 22 rutas, pero la aceptación exige revisar con PostgreSQL, cuentas privilegiadas y fixtures reales. Un archivo importable no prueba que los casos hayan pasado.

## Convenciones

- Entorno local usa `http://localhost:8080` por defecto; cambia `baseUrl` si tu despliegue E2E usa otro puerto.
- Los códigos de evento, venue, tickets y correos deben ser propios de la ejecución. No se asumen IDs ni seeds fijos.
- Respuesta negativa: comprobar el contrato `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `details`) y que no haya mutación; confirmar con una lectura autorizada posterior.
- Si falta actor, token o fixture, marcar `BLOCKED`; no simular PASS. No ejecutar la colección con Runner.

## Cobertura

Las 22 rutas están declaradas en `checklist-e2e-manual.md`. Los escenarios de seguridad requieren envíos adicionales a esas mismas rutas con diferentes actores; la cifra 22 cuenta rutas, no casos. Capturar evidencias sin contraseñas, tokens ni información personal real.
