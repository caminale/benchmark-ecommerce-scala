#!/bin/bash
email="localoca@octo.com"
node=$1
case "$node" in
    "node1")
              cockroach start \
              --insecure \
              --store=node1 \
              --host=localhost; echo start first cockroach node
              ;;
    "node2")
             cockroach start \
             --insecure \
             --store=node2 \
             --host=localhost \
             --port=26258 \
             --http-port=8081 \
             --join=localhost:26257;echo start second cockroach node
             ;;
    "node3")
            cockroach start \
            --insecure \
            --store=node3 \
            --host=localhost \
            --port=26259 \
            --http-port=8082 \
            --join=localhost:26257;echo start third cockroach node
            ;;
    "init")
            echo see the cockroach dashbord on : http://localhost:8080 &&
            echo connect to the first node to create database and table &&
            cockroach sql --insecure -e 'CREATE DATABASE octo;' &&
            cockroach sql --insecure -e 'CREATE TABLE octo.accounts (id VARCHAR(20) PRIMARY KEY,name TEXT,email TEXT);' &&
            cockroach sql --insecure -e 'SELECT * FROM octo.accounts;' &&
            cockroach user set nad --insecure &&
            cockroach sql --insecure -e 'GRANT ALL ON DATABASE octo TO nad;'
            ;;

esac



CREATE TABLE accounts (id VARCHAR(20) PRIMARY KEY,name TEXT,email TEXT,balance DECIMAL);
curl -H "Content-Type: application/json" -X POST -d '{"id":"id-2","name":"lolo","email":"octo.octo2.com","balance":200}' http://localhost:9999/user/add