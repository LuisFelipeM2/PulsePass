# Bitácora de ejecución manual E2E

En esta bitácora registro las solicitudes que ejecuté manualmente con **Send** en Postman, junto con el resultado observado y los datos necesarios para repetir cada prueba. Para proteger las cuentas, no incluyo contraseñas, JWT, tokens ni datos personales reales. Marco como `NOT RUN` los casos que todavía no he ejecutado.

## Datos de la corrida

| Campo | Valor |
|---|---|
| Fecha y hora (con zona) | pendiente |
| Persona que ejecutó las pruebas | pendiente |
| Versión o commit de PulsePass | pendiente |
| `runId` | pendiente |
| URL y puerto E2E (sin secretos) | pendiente |
| ¿Se confirmó que la base de datos es exclusiva para E2E? | sí / no |
| Migraciones aplicadas | sí / no |
| ¿Estaban listos los datos y actores de prueba? | sí / no — anotar referencia, nunca credenciales |
| Versiones de Java, PostgreSQL y Postman | pendiente |

## Resultados por solicitud

| Caso / ruta | Actor | Datos de prueba no sensibles | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
|---|---|---|---:|---:|---|---|---|
| E2E-VEN-01 GET `/api/venues/{code}` | ANONYMOUS | código `E2E-SM-001` | 200 | 200 | `Venue PulsePass E2E`, activo, capacidad 500 | PASS | Consulté el venue y confirmé que el código y los datos corresponden al registro de prueba. |
| E2E-VEN-02 GET `/api/venues/active` | ANONYMOUS | venue `E2E-SM-001` | 200 | 200 | La lista incluye el venue activo `E2E-SM-001`, capacidad 500 | PASS | Confirmé que el venue esperado aparece en la lista. |
| E2E-ART-01 GET `/api/artists/{id}` | ANONYMOUS | ID `1` (`Solar Beat`) | 200 | 200 | Consulta del artista `Solar Beat`, Colombia, Electronica, activo | PASS | Consulté el artista y confirmé sus datos de prueba. |
| E2E-ART-02 GET `/api/artists/by-stage-name` | ANONYMOUS | `Solar Beat` | 200 | 200 | La consulta corresponde al artista ID 1, Colombia, Electronica, activo | PASS | Busqué `Solar Beat` y confirmé que corresponde al artista esperado. |
| E2E-ART-03 GET `/api/artists/active` | ANONYMOUS | IDs 1–5; cinco artistas activos | 200 | 200 | JSON con 5 artistas (`Solar Beat`, `Caribbean Sound`, `Neon Waves`, `Ocean Drive`, `Digital Pulse`) | PASS | Revisé los cinco artistas y confirmé que sus IDs y nombres coinciden con los datos de prueba. |
| E2E-EVT-01 POST `/api/events` | ADMIN | código `E2E-EVT-20261018-01`, venue `E2E-SM-001` | 201 | 201 | Evento `PulsePass E2E 20261018` creado | PASS (HTTP) | Creé el evento y recibí 201. No conservé el body completo como evidencia. |
| E2E-EVT-02 GET `/api/events/{eventCode}` | ANONYMOUS | evento `E2E-EVT-20261018-01` | 200 publicado | 200 | Evento publicado consultable sin autenticación | PASS | Consulté el evento publicado sin token y recibí 200. |
| E2E-EVT-03 GET `/api/events/published` | ANONYMOUS | evento `E2E-EVT-20261018-01` | 200 | 200 | Aparece `PulsePass E2E 20261018` | PASS | Confirmé que el evento aparece en la lista de publicados. |
| E2E-EVT-04 PATCH `.../publish` | ADMIN | evento `E2E-EVT-20261018-01` | 200 | 200 | Publicación aceptada | PASS (HTTP) | Envié la solicitud de publicación como ADMIN y recibí 200. |
| E2E-EVT-05 POST `.../artists/{artistId}` | ADMIN | evento `E2E-EVT-20261018-01`, artista ID `1` | 200 | 200 | Asociación aceptada | PASS (HTTP) | Asocié el artista ID 1 al evento y recibí 200. |
| E2E-EVT-06 GET `/api/events/by-artist` | ANONYMOUS | artista `Solar Beat` | 200 | 200 | La respuesta incluye `PulsePass E2E 20261018` | PASS | Después de asociar a `Solar Beat`, consulté los eventos por artista y encontré el evento. |
| E2E-USR-01 POST `/api/users` | ANONYMOUS / USER_A / USER_B | USER_A `e2e_user_a_20261018`; USER_B `e2e_user_b_20261018`; correos `example.test` | 201 | 201 ambos | Ambos usuarios quedaron creados y activos; el JSON no expone password/hash | PASS | Registré USER_A y USER_B. Después confirmé sus roles mediante login y `/api/auth/me`. |
| E2E-USR-02 GET `/api/users/by-email` | USER_A propio; USER_B ajeno | correo de USER_A | 200 propio / 404 ajeno | 200 / 404 | USER_A obtiene su UserResponse; USER_B no ve el recurso ajeno | PASS | Consulté el correo con USER_A y USER_B; confirmé que USER_B no pudo ver el recurso ajeno. |
| E2E-USR-03 GET `/api/users/by-username` | USER_A propio; USER_B ajeno | username de USER_A | 200 propio / 404 ajeno | 200 / 404 | USER_A obtiene su UserResponse; USER_B no ve el recurso ajeno | PASS | Consulté el username con ambos usuarios y confirmé que USER_B recibió 404. |
| E2E-TKT-01 POST `/api/tickets` | USER_A | USER_A + evento `E2E-EVT-20261018-01`, type `GENERAL` | 201 | 500 en el primer intento; 201 al repetir tras la corrección | Con el GET posterior confirmé un código de 30 caracteres, type `GENERAL`, estado `PAID` y el evento correcto | PASS tras corrección | En el primer intento, el código generado tenía 40 caracteres, pero la columna admite 30 (`varchar(30)`). Después de ajustar el generador en `TicketServiceImpl`, repetí la compra y confirmé con un GET que el ticket quedó guardado. |
| E2E-TKT-02 GET `/api/tickets/{ticketCode}` | USER_A propietario; USER_B ajeno | ticket `TKT-74257a9ce8404c70a42d9f8c9c` | USER_A: 200; USER_B: 404 | 200 / 404 | USER_A consultó el ticket; USER_B recibió 404 | PASS | Consulté el ticket con USER_A y luego con USER_B; el acceso ajeno quedó oculto según la política anti-IDOR. |
| E2E-TKT-03 GET `/api/tickets/by-user` | USER_A propio | correo USER_A `user.a.20261018@example.test` | 200 | 200 | La lista contiene `TKT-74257a9ce8404c70a42d9f8c9c`, `PAID`, evento `E2E-EVT-20261018-01` | PASS | Consulté los tickets de USER_A y confirmé que la compra quedó asociada al usuario y al evento esperados. |
| E2E-TKT-04 GET `.../tickets/paid` | STAFF | evento `E2E-EVT-20261018-01` | 200 | 200 | La lista incluye el ticket de USER_A en estado `PAID` | PASS | Como STAFF, consulté el listado y confirmé que incluía el ticket `PAID` de USER_A. |
| E2E-TKT-05 PATCH `.../cancel` | propietario | ticket `TKT-74257a9ce8404c70a42d9f8c9c` | 200 | 200 | `CANCELLED` | PASS | Como USER_A, cancelé el primer ticket y confirmé el estado `CANCELLED`. |
| E2E-TKT-06 PATCH `.../use` | STAFF/ADMIN | ticket `TKT-1642df2d3ec4422a9713ae0a9a` | 200 | 200 | `USED` | PASS | Compré un segundo ticket (201) y, con STAFF, lo marqué como usado. La respuesta indicó `USED`. |
| E2E-AUTH-01 POST `/api/auth/login` | USER_A / USER_B / STAFF / ADMIN | cuentas de prueba; USER_A probó username y correo | 200; contraseña incorrecta 401 | 200 para los actores; 401 con contraseña incorrecta | Inicié sesión como USER_A con username y correo, y también como USER_B, STAFF y ADMIN | PASS | Guardé los tokens en las variables locales de Postman y no los incluí en esta bitácora. |
| E2E-AUTH-02 GET `/api/auth/me` | USER_A / USER_B / STAFF / ADMIN | identidad de cada actor | 200 | 200 los cuatro | USER_A/USER_B `ROLE_USER`; STAFF `ROLE_STAFF`; ADMIN `ROLE_ADMIN` | PASS | Consulté `/api/auth/me` con cada actor y confirmé el rol correspondiente. No registré tokens. |

## Matriz de cobertura por ruta y rol

En esta matriz uso `PASS` cuando observé el resultado indicado, `PENDIENTE` cuando todavía debo ejecutar o confirmar esa combinación y `N/A` cuando el rol no aplica al caso nominal. Para dar un caso por aprobado, también reviso el JSON y, si hice una operación de escritura, consulto después el estado del recurso.

| Caso / operación | ANONYMOUS | USER_A | USER_B | STAFF | ADMIN |
|---|---|---|---|---|---|
| E2E-VEN-01 GET venue por código | PASS 200 | N/A | N/A | N/A | N/A |
| E2E-VEN-02 GET venues activos | PASS 200 | N/A | N/A | N/A | N/A |
| E2E-ART-01 GET artista por ID | PASS 200 | N/A | N/A | N/A | N/A |
| E2E-ART-02 GET artista por nombre | PASS 200 | N/A | N/A | N/A | N/A |
| E2E-ART-03 GET artistas activos | PASS 200 | N/A | N/A | N/A | N/A |
| E2E-EVT-01 POST crear evento | PASS 401 | PASS 403 | PENDIENTE | PENDIENTE | PASS 201 |
| E2E-EVT-02 GET evento por código (publicado) | PASS 200 | N/A | N/A | N/A | PENDIENTE |
| E2E-EVT-02 GET evento draft (control de acceso) | PENDIENTE | N/A | N/A | N/A | PENDIENTE |
| E2E-EVT-03 GET publicados | PASS 200 | N/A | N/A | N/A | N/A |
| E2E-EVT-04 PATCH publicar | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PASS 200 |
| E2E-EVT-05 POST asociar artista | PENDIENTE | PENDIENTE | PENDIENTE | PENDIENTE | PASS 200 |
| E2E-EVT-06 GET eventos por artista | PASS 200 | N/A | N/A | N/A | PENDIENTE |
| E2E-USR-01 POST registrar A/B | PASS 201 (ambos) | N/A | N/A | N/A | N/A |
| E2E-USR-02 GET usuario por email | N/A | PASS 200 propio | PASS 404 ajeno | N/A | PENDIENTE |
| E2E-USR-03 GET usuario por username | N/A | PASS 200 propio | PASS 404 ajeno | N/A | PENDIENTE |
| E2E-TKT-01 POST comprar ticket propio | N/A | PASS 201 | PENDIENTE compra con email de B | N/A | N/A |
| E2E-TKT-02 GET ticket por código | N/A | PASS 200 propietario | PASS 404 ajeno | PENDIENTE | PENDIENTE |
| E2E-TKT-03 GET tickets por email | N/A | PASS 200 propio | PENDIENTE consulta ajena | N/A | PENDIENTE |
| E2E-TKT-04 GET tickets pagados | N/A | PENDIENTE (403 esperado) | PENDIENTE | PASS 200 | PENDIENTE |
| E2E-TKT-05 PATCH cancelar | N/A | PASS 200 (`CANCELLED`) | PENDIENTE ajeno | N/A | PENDIENTE |
| E2E-TKT-06 PATCH marcar usado | N/A | PENDIENTE (403 esperado) | PENDIENTE | PASS 200 (`USED`) | PENDIENTE |
| E2E-AUTH-01 POST login | N/A | PASS 200 | PASS 200 | PASS 200 | PASS 200 |
| E2E-AUTH-02 GET `/api/auth/me` | PENDIENTE sin token | PASS 200 | PASS 200 | PASS 200 | PASS 200 |

## Casos negativos y de integridad

En esta sección registro las pruebas negativas y adicionales: datos inválidos (400), token ausente o inválido (401), rol insuficiente (403), recurso inexistente o draft oculto (404), duplicados o transiciones no permitidas (409), acceso a recursos ajenos, compra con el correo de otra persona y repetición de cancelación o uso. Cuando una operación se rechaza, consulto el recurso con un actor autorizado para comprobar que su estado no cambió.

| ID de caso | Ruta | Actor | HTTP esperado/observado | ErrorResponse revisado | Lectura posterior sin mutación | PASS/FAIL/BLOCKED | Evidencia / defecto |
|---|---|---|---|---|---|---|---|
| TC-SEC-001 | POST `/api/events` | USER_A (`ROLE_USER`) | 403 / 403 | Observé 403; todavía debo revisar el ErrorResponse completo | Debo consultar el recurso con un actor autorizado y confirmar que no cambió | PASS por status 403; revisión pendiente | Como USER_A, no pude crear el evento. No adjunto el JWT. |
| TC-AUTH-007 | POST `/api/events` | ANONYMOUS | 401 / 401 | Observé 401; todavía debo revisar el ErrorResponse completo | La solicitud no ejecutó la operación protegida | PASS por status 401; revisión pendiente | Envié la solicitud sin encabezado Authorization. |
| TC-AUTH-008 | POST `/api/events` | Bearer inválido | 401 / 401 | Observé 401; todavía debo revisar el ErrorResponse completo | La solicitud no ejecutó la operación protegida | PASS por status 401; revisión pendiente | No guardé el token de prueba en la bitácora. |
| TC-AUTH-004 | POST `/api/auth/login` | USER_A, contraseña incorrecta | 401 / 401 | Observé 401; todavía debo confirmar que el mensaje sea genérico | No aplica | PASS por status 401; revisión pendiente | El login rechazó la contraseña incorrecta. |

## Consolidación de la corrida

| Resultado | Cantidad |
|---|---:|
| PASS (ruta nominal manual) | Debo revisar las aserciones; registré un status para cada una de las 22 rutas |
| FAIL (incluye ID y defecto) | Debo revisar todos los escenarios |
| BLOCKED (precondición faltante) | Debo revisar todos los escenarios |
| NOT RUN | Debo identificar los escenarios que no ejecuté |
| Negativos ejecutados | 4 |

### Defectos / regresiones

| ID | Requisito/caso | Pasos para reproducir | Esperado | Observado | Estado/severidad |
|---|---|---|---|---|---|
| Por completar | Por completar | Por completar | Por completar | Por completar | Por completar |

### Bloqueos y aceptación

- Registré un status HTTP para cada una de las 22 rutas. Antes de dar por terminada la corrida, debo revisar las respuestas y completar los escenarios adicionales pendientes.
- Errores HTTP 400/401/403/404/409 comprobados según aplique: pendiente.
- Verifiqué el login y `/api/auth/me` para USER_A, USER_B, STAFF y ADMIN.
- Debo completar las pruebas de propiedad/anti-IDOR y confirmar que las operaciones rechazadas no modificaron los datos.
- Debo completar y revisar las evidencias sin secretos y la matriz requisito → caso → ruta → resultado.
- Mi decisión final queda como **NO EVALUADO** hasta que revise todos los resultados y documente cualquier defecto o bloqueo.
