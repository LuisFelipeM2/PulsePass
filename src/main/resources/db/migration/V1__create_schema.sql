-- Reglas aplicadas: BR-001 a BR-009, NFR-001, seccion 8 (integridad)

-- BR-002
CREATE TABLE venues (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    address     VARCHAR(200),
    capacity    INTEGER      NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE, 

    CONSTRAINT uq_venues_code UNIQUE (code),  -- FR-VEN-002 / BR-009         
    CONSTRAINT ck_venues_capacity CHECK (capacity > 0) -- FR-VEN-003
)

-- BR-001: todo Event pertenece a exactamente un Venue -> FK NOT NULL 

CREATE TABLE events (
    id              BIGSERIAL PRIMARY KEY,
    event_code      VARCHAR(30) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    category        VARCHAR(30)  NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    event_date      TIMESTAMP    NOT NULL,
    minimum_age     INTEGER      NOT NULL DEFAULT 0,
    venue_id        BIGINT       NOT NULL,

    CONSTRAINT uq_events_event_code UNIQUE (event_code), -- FR-EVT-002
    CONSTRAINT fk_events_venue FOREIGN KEY (venue_id)
        REFERENCES venues (id),
    CONSTRAINT ck_events_category CHECK (category IN ( 
        'MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT'
    )), -- FR-EVT-004
    CONSTRAINT ck_events_status CHECK (status IN (   
        'DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'
    )), -- FR-EVT-003
    CONSTRAINT ck_events_minimum_age CHECK (minimum_age >= 0)
)

CREATE INDEX idx_events_venue_id ON events (venue_id); -- FR-VEN-004
CREATE INDEX idx_events_status_date ON events (status, event_date); -- FR-EVT-005


-- BR-003: Event <-> Artist, cero o muchos en ambos sentidos 
-- PK compuesta = evita duplicar el mismo par evento-artista (FR-ART-003)

CREATE TABLE artists (
    id          BIGSERIAL PRIMARY KEY,
    stage_name  VARCHAR(150) NOT NULL,
    country     VARCHAR(100),
    genre       VARCHAR(100),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_artists_stage_name UNIQUE (stage_name) -- FR-ART-002
);

CREATE TABLE event_artists (
    event_id    BIGINT NOT NULL,
    artist_id   BIGINT NOT NULL,

    CONSTRAINT pk_event_artists PRIMARY KEY (event_id, artist_id),
    CONSTRAINT fk_event_artists_event FOREIGN KEY (event_id)
        REFERENCES events (id),
    CONSTRAINT fk_event_artists_artist FOREIGN KEY (artist_id)
        REFERENCES artists (id)
);

CREATE INDEX idx_event_artists_artist_id ON event_artists (artist_id); -- FR-ART-004

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(150) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_users_username UNIQUE (username), -- FR-USR-002
    CONSTRAINT uq_users_email UNIQUE (email) -- FR-USR-002  
);

-- BR-004: un usuario puede tener MAXIMO un UserProfile
CREATE TABLE user_profiles (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    phone       VARCHAR(30),
    city        VARCHAR(100),
    birth_date  DATE,
    user_id     BIGINT NOT NULL,

    CONSTRAINT uq_user_profiles_user_id UNIQUE (user_id), -- clave del 1:1, FR-USR-003 / AC-004
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id)
);

-- BR-005/006: Ticket es entidad propia con datos propios 
-- BR-007/NFR-008: price como NUMERIC (nunca float/double)
CREATE TABLE tickets (
    id              BIGSERIAL PRIMARY KEY,
    ticket_code     VARCHAR(30)     NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    price           NUMERIC(10, 2)  NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    purchase_date   TIMESTAMP       NOT NULL DEFAULT now(),
    user_id         BIGINT          NOT NULL,
    event_id        BIGINT          NOT NULL,

    CONSTRAINT uq_tickets_ticket_code UNIQUE (ticket_code), -- FR-TKT-002
    CONSTRAINT fk_tickets_user FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_tickets_event FOREIGN KEY (event_id)
        REFERENCES events (id),

    CONSTRAINT ck_tickets_price CHECK (price >= 0), -- FR-TKT-003         
    CONSTRAINT ck_tickets_type CHECK (type IN (             
        'GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT'
    )), -- FR-TKT-004
    CONSTRAINT ck_tickets_status CHECK (status IN (          
        'RESERVED', 'PAID', 'CANCELLED', 'USED'
    )) -- FR-TKT-005
);

CREATE INDEX idx_tickets_user_id ON tickets (user_id); -- FR-TKT-006            
CREATE INDEX idx_tickets_event_id_status ON tickets (event_id, status); -- FR-TKT-007/008