i# Gigaspaces Kafka Sink Connector

## Prerequisite
- Installation of Kafka and Kafka Connect  
- Installation of Insightedge v15.2
- Git, Maven and JDK 8

## Install
- git clone this repo
- mvn clean package
- Move the generated jar (from the target folder) to the kafka connect connectors lib folder
- Define the connector configuration as outlined below
- Schema and type definitions for the data can be expressed via json file as seen below, or by providing a jar file with the pojo classes that define the schema.

## Configuration
Gigaspaces connector properties file Example:
```
bootstrap.servers=localhost:9092
name=gigaspaces-kafka
connector.class=com.gigaspaces.kafka.connector.GigaspacesSinkConnector
tasks.max=1
topics=mytype
### giggaspaces specific
gs.connector.name=gs
gs.space.embedded=false
gs.space.name=demo
gs.space.locator=127.0.0.1:4174
###
### gigaspaces type schema can be defined in a json file or a model jar
# gs.model.jar.path=/home/vagrant/gigaspaces-kafka-connector/acme-model-0.1.jar
gs.model.json.path=/home/vagrant/gigaspaces-kafka-connector/model.json
###
plugin.path=/home/vagrant/gigaspaces-kafka-connector/
value.converter=org.apache.kafka.connect.json.JsonConverter
value.converter.schemas.enable=false
key.converter=org.apache.kafka.connect.storage.StringConverter
key.converter.schemas.enable=false
offset.storage.file.filename=/tmp/connect.offsets
# Flush much faster than normal, which is useful for testing/debugging
offset.flush.interval.ms=10000
```

## Gigaspaces connector model schema json file example:
```json
[{
	"type": "com.gs.Person",
	"FixedProperties": {
	"firstname": "java.lang.String",
	"lastname": "java.lang.String",
	"age": "java.lang.Integer",
	"num": "java.lang.Integer"
	},
	"Indexes": {
	"compoundIdx": {"type":"EQUAL", "properties": ["firstname", "lastname"], "unique": false},
	"ageIdx": {"type":"ORDERED", "properties": ["age"], "unique": false}
	},
	"Id": "num",
	"RoutingProperty": "firstname"
},
{
	"type": "com.gs.Pet",
	"FixedProperties": {
	"kind": "java.lang.String",
	"name": "java.lang.String",
	"age": "java.lang.Integer"
	},
	"Indexes": {
	"compoundIdx": {"type":"EQUAL", "properties": ["kind", "name"], "unique": false},
	"ageIdx": {"type":"ORDERED", "properties": ["age"], "unique": false}
	},
	"RoutingProperty": "name"
}]
```

## Running the example:
In this example, we will consume data from text file using the FileStreamSource source connector.
This connector will publish the lines it read to the type topics in Kafka. 
The Gigaspaces sink connector will read the data from the topics and store them in the in-memory grid (aka the "space")
- all files are under the example/resources folder
- Run Gigaspaces with a partitioned space (easiet is running bin/gs.sh demo command)
- Run Kafka. (Since Gigaspaces starts with zookeeper, if you run Kafka on the same machine, use the Gigaspaces zookeeper instead of starting another instance)
- When everything is running, we can start the connect with the source and sink connectors and see how the data is consumed and published to the space.
- Connecto to the gigaspaces UI and view the types that were defined and the data that was inserted in to the spaces by the connector.


