CREATE TABLE employee
(
 id uuid DEFAULT gen_random_uuid(),
 name VARCHAR NOT NULL,
 PRIMARY KEY (id)
);