-- FR-EVT-006: agrega URL de streaming opcional a eventos.

ALTER TABLE events ADD COLUMN streaming_url VARCHAR(500); 