# Gigaspaces Kafka Sink Connector

## Prerequisite
- Installation of Kafka and Kafka Connect  
- Installation of Insightedge v15.2
- Git, Maven and JDK 8

## Install
- git clone this repo
- mvn clean package
- Move the generated jar (from the target folder) to the kafka connect connectors lib folder
- Define the connector configuration as outlined below
- Schema and type definitions for the data can be expressed via the json file as shown below.

**Note:** If you have developed a GigaSpaces data model, you do not have to provide a json file. Instead, you can provide the generated jar file containing the relevant POJOs.

## Configuration
### Gigaspaces connector properties file example:



```
bootstrap.servers=localhost:9092
name=gigaspaces-kafka
connector.class=com.gigaspaces.kafka.connector.GigaspacesSinkConnector
tasks.max=1
topics=Pet,Person
gs.connector.name=gs
# True -- start gs inside the same JVM as connector; False - separate JVM (default)
gs.space.embedded=false
# Name of the target gs Space
gs.space.name=demo
# Location of GS Manager:
gs.space.locator=127.0.0.1:4174
#Choose one of the following -- Jar file or Json file: 
#gs.model.jar.path=/home/vagrant/gigaspaces-kafka-connector/acme-model-0.1.jar
gs.model.json.path=/home/vagrant/gigaspaces-kafka-connector/example/resources/model.json
#
plugin.path=/home/vagrant/gigaspaces-kafka-connector/

value.converter=org.apache.kafka.connect.json.JsonConverter
value.converter.schemas.enable=false

key.converter=org.apache.kafka.connect.storage.StringConverter
# Currently the connector does not support Kafka schema.
key.converter.schemas.enable=false
#key.converter.schemas.enable=true
#value.converter.schemas.enable=true

offset.storage.file.filename=/tmp/connect.offsets
# Flush much faster than normal, which is useful for testing/debugging
offset.flush.interval.ms=10000

```

### Gigaspaces connector model schema json file example
**Note:** These Json fields map to the Space Type Descriptor in GS.
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
**Note:** The  steps must be run in the order indicated below.

**Note:** The examples below are for Windows.

 In this example, we will consume data from a text file using the FileStreamSource source connector.
This connector will publish the lines it reads to the type topics in Kafka. 
The Gigaspaces sink connector will read the data from the topics and store them in the in-memory grid (the "space").
All files are under the example/resources folder

1.Start Gigaspaces and have a space running. In this example, we are running the demo project: gs.bat demo

2.Start Zookeeper on a port different from the port that GigaSpaces is using. If GigaSpaces is running on port 2181 (the default in this example), use port 2182:
zookeeper-server-start.bat ..\..\zookeeper.properties

3.Start kafka using the different port above. e.g. 2182. Note that Zookeeper was already started by GigaSpaces.
```
    (kafka root)\bin\windows\kafka-server-start.bat ..\..\config\server.properties
```
4.When everything is running, we can start the connect with the source and sink connectors and see how the data is consumed and published to the space:

(kafka root)\bin\windows\connect-standalone.bat ..\..\config\connect-standalone.properties 
   
    (gs root)\gigaspaces-kafka-connector\example\resources\people-source.properties  

    (gs root)\gigaspaces-kafka-connector\example\resources\pet-source.properties

    (gs root)\gigaspaces-kafka-connector\example\resources\connect-gigaspaces-sink.properties


5.Connect to the gigaspaces UI and view the types that were defined and the data that was inserted in to the spaces by the connector.


