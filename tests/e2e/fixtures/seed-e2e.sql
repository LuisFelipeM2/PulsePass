-- Solo para la base local E2E dedicada (pulsepass_e2e).
-- Crea/actualiza un venue activo necesario para los flujos de eventos.
-- No ejecutar contra desarrollo compartido, QA ni producción.
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('E2E-SM-001', 'Venue PulsePass E2E', 'Santa Marta', 'Zona de pruebas 1', 500, TRUE)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    city = EXCLUDED.city,
    address = EXCLUDED.address,
    capacity = EXCLUDED.capacity,
    active = EXCLUDED.active;
