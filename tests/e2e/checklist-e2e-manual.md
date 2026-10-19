# Checklist manual E2E — 22 rutas

Enviar cada solicitud desde Postman manualmente; no usar Runner. Los IDs `E2E-*` identifican rutas, no el número total de escenarios. Para autorización y propiedad repetir solicitudes existentes con los actores indicados. Usar variables del environment local y copiar IDs/tokens a mano.

## Secuencia de ejecución

1. [ ] Arrancar API y PostgreSQL E2E dedicados; migraciones Flyway aplicadas.
2. [ ] Preparar venue/artista activos, evento draft, evento publicado futuro, evento por publicar, ticket elegible y actores USER_A, USER_B, STAFF y ADMIN. Aislar datos por `runId`.
3. [ ] Importar `postman/PulsePass-Manual-E2E.postman_collection.json` y `postman/PulsePass-local.postman_environment.json`; seleccionar el entorno.
4. [ ] Ejecutar solicitudes de lectura pública para descubrir y anotar códigos/IDs reales.
5. [ ] Ejecutar `POST /api/users` para USER_A y repetirlo manualmente para USER_B. Luego login de cada cuenta, STAFF y ADMIN; copiar `accessToken` a la variable local del actor. Ejecutar `/api/auth/me` con cada token.
6. [ ] Ejecutar creación/asociación/publicación con ADMIN y verificar borrador/publicación con lecturas públicas y privilegiadas.
7. [ ] Ejecutar compra/consulta/cancelación/uso con tickets separados. Después de cada operación irreversible, hacer GET de estado con actor autorizado.
8. [ ] Ejecutar matriz de negativas (sin token, token inválido, rol insuficiente, usuario ajeno, recurso inexistente, validación inválida y transición inválida). Registrar status exacto, ErrorResponse y lectura posterior sin mutación.
9. [ ] Completar bitácora por cada envío. Usar BLOCKED si falta fixture/token/precondición y FAIL si la respuesta real contradice el contrato.

## Matriz de rutas

| ID | Método y ruta | Actor nominal | Esperado | Verificación manual |
|---|---|---|---:|---|
| E2E-VEN-01 | GET `/api/venues/{code}` | ANONYMOUS | 200 | Venue coincide con fixture; código ausente 404 |
| E2E-VEN-02 | GET `/api/venues/active` | ANONYMOUS | 200 | Todos los resultados activos |
| E2E-ART-01 | GET `/api/artists/{id}` | ANONYMOUS | 200 | ID existente; ausente 404 |
| E2E-ART-02 | GET `/api/artists/by-stage-name?stageName=...` | ANONYMOUS | 200 | Artista coincide con fixture |
| E2E-ART-03 | GET `/api/artists/active` | ANONYMOUS | 200 | Solo artistas activos |
| E2E-EVT-01 | POST `/api/events` | ADMIN | 201 | DRAFT inicial, código recuperable |
| E2E-EVT-02 | GET `/api/events/{eventCode}` | ANONYMOUS / ADMIN | 200 | Publicado visible; draft oculto al público como 404 |
| E2E-EVT-03 | GET `/api/events/published` | ANONYMOUS | 200 | Sin borradores |
| E2E-EVT-04 | PATCH `/api/events/{eventCode}/publish` | ADMIN | 200 | Releer y confirmar PUBLISHED |
| E2E-EVT-05 | POST `/api/events/{eventCode}/artists/{artistId}` | ADMIN | 200 | Relación visible en DTO/consulta posterior |
| E2E-EVT-06 | GET `/api/events/by-artist?stageName=...` | ANONYMOUS / ADMIN | 200 | Público solo publicados; comparar ADMIN |
| E2E-USR-01 | POST `/api/users` | ANONYMOUS | 201 | Rol inicial USER; duplicado 409; repetir para B |
| E2E-USR-02 | GET `/api/users/by-email?email=...` | propietario / ADMIN | 200 | Usuario ajeno 404 anti-enumeración según AuthorizationService |
| E2E-USR-03 | GET `/api/users/by-username?username=...` | propietario / ADMIN | 200 | Usuario ajeno 404 anti-enumeración según AuthorizationService |
| E2E-TKT-01 | POST `/api/tickets` | USER propietario | 201 | Email de A con token A; token A + email B denegado y sin ticket |
| E2E-TKT-02 | GET `/api/tickets/{ticketCode}` | propietario / STAFF / ADMIN | 200 | Ticket ajeno 404 anti-enumeración según AuthorizationService |
| E2E-TKT-03 | GET `/api/tickets/by-user?email=...` | propietario / ADMIN | 200 | Consulta ajena 404 anti-enumeración |
| E2E-TKT-04 | GET `/api/events/{eventCode}/tickets/paid` | STAFF / ADMIN | 200 | Solo PAID; USER/ANONYMOUS denegados |
| E2E-TKT-05 | PATCH `/api/tickets/{ticketCode}/cancel` | propietario / ADMIN | 200 | PAID→CANCELLED; ajeno 404 sin mutación; repetir 409 |
| E2E-TKT-06 | PATCH `/api/tickets/{ticketCode}/use` | STAFF / ADMIN | 200 | PAID→USED; USER denegado; repetir 409 |
| E2E-AUTH-01 | POST `/api/auth/login` | ANONYMOUS | 200 | Repetir por actor; revisar tokenType/expiresIn; credencial mala 401 genérico |
| E2E-AUTH-02 | GET `/api/auth/me` | autenticado | 200 | Identidad y roles del token actual; sin token/token inválido 401 |

## Matriz mínima negativa

| Categoría | Ejecución | Resultado esperado |
|---|---|---|
| 400 | Registro/request con campo requerido ausente, email inválido, JSON o enum inválido | ErrorResponse, sin mutación |
| 401 | Ruta protegida sin Bearer, Bearer malformado/alterado, login incorrecto | ErrorResponse; no revelar existencia de cuenta |
| 403 | USER/STAFF intentando acción ADMIN o USER intentando usar ticket | ErrorResponse y estado sin cambios |
| 404 | Código de venue/artista inexistente; draft visto por público | ErrorResponse, sin filtrar borrador |
| 409 | Usuario duplicado, regla de compra/estado, repetición de transición | ErrorResponse; ninguna mutación indebida |
| Propiedad | USER_B consulta/cancela ticket o usuario de USER_A; USER_A compra con email B | Recursos ajenos 404; compra con email ajeno 403; comprobar GET posterior |

> El PRD pide decisión explícita 403 vs 404 por recurso. No aceptar ambos como equivalentes: fijar el esperado según el PRD de Seguridad/implementación y registrar cualquier divergencia como defecto.

## Trazabilidad requisito → caso → ruta → resultado

Completar el último campo en la bitácora después de la ejecución. El resultado inicial es `NOT RUN`; no inferir aprobación por tener la solicitud en la colección.

| Requisito Fase 5 | Caso(s) | Ruta(s) representativas | Resultado/evidencia |
|---|---|---|---|
| FR-E2E-001 Colección Postman | E2E-VEN-01 … E2E-AUTH-02 | 22 rutas listadas arriba | Importación manual; estado en bitácora |
| FR-E2E-002 Entorno parametrizable | Todos | `{{baseUrl}}` y variables locales | Environment importado sin secretos exportados |
| FR-E2E-003 Autenticación manual | E2E-USR-01, E2E-AUTH-01/02 | `/api/users`, `/api/auth/login`, `/api/auth/me` | Login y `/me` de USER_A/B, STAFF, ADMIN |
| FR-E2E-004 Token por actor | E2E-AUTH-01/02 | `/api/auth/login`, `/api/auth/me` | Tokens copiados manualmente a variables locales |
| FR-E2E-005 Comprobación manual | Todos | Las 22 rutas | Status, JSON y efecto de negocio anotados |
| FR-E2E-006 Encadenamiento manual | E2E-EVT-01/04, E2E-TKT-01/02/05/06 | Events y tickets | Códigos copiados manualmente y reconsultados |
| FR-E2E-007 Matriz de autorización | E2E-EVT-01/04/05, E2E-USR-02/03, E2E-TKT-01…06 | Endpoints protegidos y recursos ajenos | Evidencia 401/403/404 por política real |
| FR-E2E-008 Integridad funcional | E2E-EVT-01/04, E2E-TKT-01/05/06 | Events/tickets | Estado persistido tras cada transición |
| FR-E2E-009 ErrorResponse | Negativos HTTP | Rutas anteriores con inputs inválidos/no autorizados | Campos `timestamp/status/error/message/details` revisados |
| FR-E2E-010 Datos controlados | Fixtures del README | Fixture IDs usados por todos los casos | Referencia de entorno y `runId` en bitácora |
| FR-E2E-011 Instrucciones paso a paso | Checklist y descripciones Postman | Flujo A–E | Actor, precondición, acción y comprobación documentados |
| FR-E2E-012 Bitácora manual | Cada envío | Todas | Fila con HTTP esperado/real y PASS/FAIL/BLOCKED |
| FR-E2E-013 Confidencialidad | Environment/bitácora | Todos | Sin password ni JWT en archivos compartidos/evidencias |
| FR-E2E-014 Regresión | Negativos y contratos | Casos 400/401/403/404/409 | Defecto registrado si diverge de Fases 1–4 |
| FR-E2E-015 Bloqueos | Casos dependientes | Cualquier ruta con precondición faltante | Marcar BLOCKED, nunca PASS |
| FR-E2E-016 Repetibilidad | Todos | `runId` + datos aislados | Procedimiento de preparación/restauración documentado |
| FR-E2E-017 Contratos heredados | E2E-VEN…E2E-AUTH | Rutas implementadas | DTO/enums/rutas verificados en fuente y HTTP |

## Contratos confirmados en fuente

- `RegisterUserRequest`: `username`, `email`, `password`, `firstName`, `lastName`, `phone`, `city`, `birthDate`; password 12–72 caracteres, usuario activo con rol inicial definido por el servidor.
- `LoginRequest`: `identifier`, `password`. `AuthResponse`: `accessToken`, `tokenType`, `expiresIn`. `/api/auth/me`: `id`, `username`, `roles`.
- `CreateEventRequest`: `eventCode`, `name`, `description`, `category`, `eventDate`, `minimumAge`, `venueCode`; no enviar status.
- `EventCategory`: `MUSIC`, `SPORTS`, `TECHNOLOGY`, `EDUCATION`, `CULTURE`, `ENTERTAINMENT`.
- `PurchaseTicketRequest`: `userEmail`, `eventCode`, `type`; `TicketType`: `GENERAL`, `VIP`, `BACKSTAGE`, `STUDENT`. No enviar precio.
- Todo error debe revisarse contra `ErrorResponse`: `timestamp`, `status`, `error`, `message`, `details`.
