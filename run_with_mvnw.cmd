echo on
set HADOOP_HOME=%cd%\misc\hadoop
set spring.data.mongodb.uri=mongodb://localhost:27017
set spring.data.mongodb.authentication-database=none
set spring.data.mongodb.username=none
set spring.data.mongodb.password=none
set spring.data.mongodb.database=ryadb

mvnw spring-boot:run