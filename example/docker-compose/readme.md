1. Prerequisites: Docker, Docker Compose, curl, Java 11
1. Make 'docker-compose' you current directory before running the scripts.
1. Make all shell scripts executable with `chmod +x $(ls *.sh)`
1. Run `1-build-image-kafka-connect-with-gs-plugin.sh` to create kafka-connect image with gigaspaces plugin jar in it.
1. Make sure ports 8080,8081,8082,8083,8088,8090,8099,9021,9092,9101 are not being listened to on the local machine. 
   Or alternatively, run the next step and resolve port conflicts individually if they occur.
1. Run `2-docker-compose-up-and-wait.sh` to launch the environment.
  
1. The following Web applications should be accessible in the browser at this stage.
    * http://localhost:8090 - GigaSpaces Ops Manager
    * http://localhost:8099 - Gigaspaces Management Console
    * http://localhost:8080 - UI for Apache Kafka
  
1. Run `3-create-connector-people.sh` to create demo source connector for people.
1. Run `4-create-connector-pets.sh` to create demo source connector for pets.
1. After a short time, the connectors will create Person and Pet topics Kafka, 
   and push a few messages to them. Check the following URLs
    * http://localhost:8080/ui/clusters/local/topics/Person/messages
    * http://localhost:8080/ui/clusters/local/topics/Pet/messages
  
1. Run `5-create-connector-gigaspaces.sh`. 
   This connector will read the messages from Person and Pet topics and push them to Space.
   Verify that People and Pets demo data is in Space
    * http://localhost:8090/spaces/demo/object-types/com.gs.Person
    * http://localhost:8090/spaces/demo/object-types/com.gs.Pet