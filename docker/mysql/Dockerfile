FROM mysql:latest

ENV MYSQL_DATABASE='explorun'
ENV MYSQL_ROOT_PASSWORD='root'

COPY ./sql_scripts /docker-entrypoint-initdb.d/
#COPY ./mysql_data /var/lib/mysql

