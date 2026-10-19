-- Solo para la base local E2E dedicada (pulsepass_e2e).
-- Antes de ejecutarlo, registrar manualmente estas dos cuentas con POST /api/users
-- para que las contraseñas se almacenen usando BCrypt:
--   e2e_admin_20261018
--   e2e_staff_20261018
-- Este fixture reemplaza el rol inicial USER por ADMIN/STAFF.
DO $$
DECLARE
    admin_user_id BIGINT;
    staff_user_id BIGINT;
BEGIN
    SELECT id INTO admin_user_id
      FROM users
     WHERE username = 'e2e_admin_20261018' AND active = TRUE;
    SELECT id INTO staff_user_id
      FROM users
     WHERE username = 'e2e_staff_20261018' AND active = TRUE;

    IF admin_user_id IS NULL OR staff_user_id IS NULL THEN
        RAISE EXCEPTION 'Registra y activa primero e2e_admin_20261018 y e2e_staff_20261018';
    END IF;

    DELETE FROM user_roles
     WHERE user_id IN (admin_user_id, staff_user_id) AND role = 'USER';

    INSERT INTO user_roles (user_id, role)
    VALUES (admin_user_id, 'ADMIN'), (staff_user_id, 'STAFF')
    ON CONFLICT (user_id, role) DO NOTHING;
END $$;
