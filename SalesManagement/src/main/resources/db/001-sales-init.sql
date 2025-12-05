CREATE TABLE IF NOT EXISTS sales (
  id VARCHAR DEFAULT gen_random_uuid(),
  amount NUMERIC NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT(true),
  PRIMARY KEY (id)
);