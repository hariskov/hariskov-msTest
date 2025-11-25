CREATE TABLE IF NOT EXISTS users (
  id uuid DEFAULT gen_random_uuid(),
  name VARCHAR NOT NULL,
  PRIMARY KEY (id)
);