FROM postgres:16-alpine
COPY src/main/resources/db/ /docker-entrypoint-initdb.d/
RUN chown postgres:postgres /docker-entrypoint-initdb.d/*.sql