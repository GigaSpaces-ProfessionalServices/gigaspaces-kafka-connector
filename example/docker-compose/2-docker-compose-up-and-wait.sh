#!/usr/bin/env bash
set -e

# launch the environment
docker compose up -d

echo "Waiting for Kafka Connect REST API to be ready. May take 1-2 minutes."
while [ $(curl -s -o /dev/null -w %{http_code} http://localhost:8083/connectors) -eq 000 ] ; do
  echo -e $(date) " Kafka Connect listener HTTP state: " $(curl -s -o /dev/null -w %{http_code} http://kafka-connect:8083/connectors) " (waiting for 200)"
  sleep 3
done
printf "\n"
echo "Ready for the next step"
