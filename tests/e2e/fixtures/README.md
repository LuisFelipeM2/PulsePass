# Fixtures E2E locales

La Fase 5 requiere datos deterministas fuera de los endpoints de negocio. Este directorio no aplica fixtures automáticamente ni contiene credenciales. Provisiona los datos únicamente en la base E2E dedicada (`pulsepass_e2e` o nombre aprobado), después de ejecutar las migraciones Flyway vigentes.

## Datos mínimos que debe preparar el responsable del entorno

- Venue activo con `code` real; artista activo recuperable por ID y `stageName`.
- Evento DRAFT futuro y asociado al venue activo; evento PUBLISHED futuro con capacidad disponible y edad mínima compatible; otro DRAFT listo para publicar.
- Cuentas USER_A, USER_B, STAFF, ADMIN e INACTIVE_USER. Las cuentas privilegiadas deben provisionarse fuera de `POST /api/users`; guardar passwords con BCrypt. El request público no permite asignar roles.
- Tickets independientes: al menos uno `PAID` para cancelar, otro `PAID` elegible para marcar USED y uno adicional para propiedad ajena. No reutilizar un ticket ya cancelado/usado.
- Códigos/correos únicos por corrida, con prefijo o sufijo identificable por `runId` y dominio reservado de pruebas. Anotar los IDs reales en variables locales de Postman.

## Reglas de seguridad de datos

- No usar datos de producción ni compartir secrets/JWT. No guardar passwords en SQL versionado.
- `seed-e2e.sql` crea de forma repetible el venue E2E `E2E-SM-001` y solo debe ejecutarse en la base E2E dedicada. Los artistas activos existentes vienen de la migración `V2`.
- Desde PowerShell, en la raíz del repo, se puede cargar ese fixture con `Get-Content .\tests\e2e\fixtures\seed-e2e.sql | docker exec -i pulsepass-e2e psql -U postgres -d pulsepass_e2e` si el contenedor se creó con ese nombre y base. Luego consultar el venue por Postman y copiar `E2E-SM-001` a `venueActiveCode` en el environment privado.
- Para ADMIN/STAFF, registra primero por Postman dos usuarios de prueba con username `e2e_admin_20261018` y `e2e_staff_20261018`, usando passwords locales. Luego carga `seed-privileged-roles.sql` con `Get-Content .\tests\e2e\fixtures\seed-privileged-roles.sql | docker exec -i pulsepass-e2e psql -U postgres -d pulsepass_e2e`. El script exige ambas cuentas activas, cambia sus roles iniciales USER por ADMIN/STAFF y es idempotente.
- Después, configura `adminIdentifier=e2e_admin_20261018` y `staffIdentifier=e2e_staff_20261018` en el environment privado, introduce las contraseñas que registraste y haz login por separado. Verifica roles con `/api/auth/me` antes de ejecutar acciones privilegiadas.
- Estos seeds modifican únicamente la base E2E; no ejecutarlos contra desarrollo compartido, QA ni producción.
- No ejecutar reset o truncado sobre bases distintas a la base E2E explícitamente confirmada. Restaurar usando el procedimiento administrativo de pruebas aprobado.
