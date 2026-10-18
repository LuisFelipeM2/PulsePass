-- user_credentials: 1:1 con el usuario (9.1). El user_id es PK y FK a la vez,
-- así cada usuario tiene como máximo una credencial.
CREATE TABLE user_credentials (
    user_id       BIGINT       PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,   -- 9.1: el hash es obligatorio
    password_changed_at TIMESTAMP,        -- password_changed_at es opcional
    CONSTRAINT fk_user_credentials_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

-- user_roles: un usuario puede tener varios roles (9.1).
-- PK compuesta (user_id, role) y rol restringido a los tres permitidos (9.2).
CREATE TABLE user_roles (
    user_id BIGINT      NOT NULL,
    role    VARCHAR(20) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_user_roles_role
        CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

