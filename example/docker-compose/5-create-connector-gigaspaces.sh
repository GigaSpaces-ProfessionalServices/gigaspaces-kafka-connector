#!/usr/bin/env bash
# shellcheck disable=SC2016
set -e

# this will create file /tmp/gs/model.json inside the kafka-connect container
docker cp ../resources/model.json connect:/tmp/gs

# create gigaspaces sink connector via REST API
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" -d '{
     "name": "gigaspaces-kafka",
     "config": {
          "connector.class": "com.gigaspaces.kafka.connector.GigaspacesSinkConnector",
          "bootstrap.servers": "broker:9092",
          "tasks.max": "1",
          "topics": "Pet,Person",
          "gs.connector.name": "gs",
          "gs.space.embedded": "false",
          "gs.space.name": "demo",
          "gs.space.groups": "demo-lookup-group",
          "gs.space.locators": "gigaspaces:4174",
          "gs.model.json.path": "/tmp/gs/model.json",
          "value.converter": "org.apache.kafka.connect.json.JsonConverter",
          "value.converter.schemas.enable": "false",
          "key.converter": "org.apache.kafka.connect.storage.StringConverter",
          "key.converter.schemas.enable": "false",
          "offset.storage.file.filename": "/tmp/connect.offsets",
          "offset.flush.interval.ms": "10000"
          }
     }'

printf "\n\n"
echo "Done!"