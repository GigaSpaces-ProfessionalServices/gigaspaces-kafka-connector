#!/usr/bin/env bash
set -e

# build plugin jar
(cd ../.. && mvn clean package)

# docker build needs all files in current dir
cp ../../target/gigaspaces-kafka-connector-1.0.0.jar .

# build docker image with plugin jar
docker build -t kafka-connect-with-gs-plugin -f dockerfile-kafka-connect-with-gs-plugin .

# now we can remove jar file in current dir
rm gigaspaces-kafka-connector-1.0.0.jar

printf "\n"
echo "Ready for the next step"

