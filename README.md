# Gigaspaces Kafka Sink Connector

## Install
- git clone this repo
- mvn clean package
- Move the generated jar (from the target folder) to the kafka connect connectors lib folder
- Define the connector configuration

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

Gigaspaces connector model schema json file example:
```json
[{
	"type": "com.gs.mytype",
	"FixedProperties": {
	"firstname": "java.lang.String",
	"lastname": "java.lang.String",
	"num": "java.lang.Integer"
	},
	"Indexes": {
	"name": {"type":"ORDERED", "properties": ["firstname", "lastname"], "unique": false}
	},
	"Id": "num",
	"RoutingProperty": "firstname",
	"SupportsDynamicProperties": true
}]
```


