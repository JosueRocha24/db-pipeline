FROM mysql:latest

COPY include/init_script.sql /scripts/

ENTRYPOINT ["docker-entrypoint.sh"]

EXPOSE 3306 33060

CMD ["mysqld"]
