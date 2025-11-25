FROM postgres:16-alpine
COPY ../../resources/db/ /docker-entrypoint-initdb.d/
RUN chown postgres:postgres /docker-entrypoint-initdb.d/*.sql