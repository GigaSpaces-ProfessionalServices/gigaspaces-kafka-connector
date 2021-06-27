#!/usr/bin/env bash
# shellcheck disable=SC2016
set -e

# this will create file /tmp/gs/people.txt inside the kafka-connect container
docker cp ../resources/people.txt connect:/tmp/gs

# create demo source connector via REST API
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" -d '{
     "name": "people-source",
     "config": {
             "connector.class": "FileStreamSource",
             "tasks.max": "1",
             "file": "/tmp/gs/people.txt",
             "topic": "Person"
             }
     }'

printf "\n\n"
echo "Ready for the next step"