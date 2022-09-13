# Quarkus Narayana crash recovery test

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/getting-started-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Setup Mysql database
```shell script
docker run --name db-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -d -p 3306:3306 mysql

docker exec -it db-mysql mysql -uroot -proot -e \
  "CREATE DATABASE testdb CHARACTER SET utf8mb4;
   CREATE USER 'admin'@'%' IDENTIFIED WITH mysql_native_password BY 'admin';
   GRANT ALL ON testdb.* TO 'admin'@'%';
   GRANT XA_RECOVER_ADMIN on *.* to 'admin'@'%';
   FLUSH PRIVILEGES;"
```

## Create a test table
```shell script
docker exec -it db-mysql mysql -uroot -proot testdb -e \
  "CREATE TABLE audit_log ( \
  id bigint NOT NULL AUTO_INCREMENT, \
  message varchar(255) DEFAULT NULL, \
  PRIMARY KEY (id));"
```

## First run your application and send a message to make it crash
```shell script
java -jar target/quarkus-app/quarkus-run.jar
curl -X POST http://localhost:8080/hello -d "crash"
```

And your application will crash with the following error:
```
2022-09-08 22:40:15,106 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032010: JBossTS Recovery Service (tag: 518c8dd50a9ae8e70eb15dfe8fc764adcabef8ab) - JBoss Inc.
2022-09-08 22:40:15,166 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032013: Starting transaction recovery manager
2022-09-08 22:40:15,360 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.12.1.Final) started in 0.608s. Listening on: http://0.0.0.0:8080
2022-09-08 22:40:15,361 INFO  [io.quarkus] (main) Profile prod activated. 
2022-09-08 22:40:15,361 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, jdbc-mysql, narayana-jta, resteasy-reactive, smallrye-context-propagation, vertx]
2022-09-08 22:40:21,558 INFO  [org.acm.DummyXAResource] (executor-thread-0) Preparing DummyXAResource
2022-09-08 22:40:21,591 INFO  [org.acm.DummyXAResource] (executor-thread-0) Committing DummyXAResource
2022-09-08 22:40:21,592 INFO  [org.acm.DummyXAResource] (executor-thread-0) Crashing the system

```

## Restart your application and check the recovery
```shell script
java -jar target/quarkus-app/quarkus-run.jar
curl  http://localhost:8080/hello/recovery
```
And you will see the following messages:
```
2022-09-08 22:40:25,896 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032010: JBossTS Recovery Service (tag: 518c8dd50a9ae8e70eb15dfe8fc764adcabef8ab) - JBoss Inc.
2022-09-08 22:40:25,968 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032013: Starting transaction recovery manager
2022-09-08 22:40:26,185 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.12.1.Final) started in 0.662s. Listening on: http://0.0.0.0:8080
2022-09-08 22:40:26,187 INFO  [io.quarkus] (main) Profile prod activated. 
2022-09-08 22:40:26,187 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, jdbc-mysql, narayana-jta, resteasy-reactive, smallrye-context-propagation, vertx]
current audit_log entries: 0
2022-09-08 22:40:36,238 INFO  [org.acm.DummyXAResourceRecovery] (Periodic Recovery) DummyXAResourceRecovery returning list of resources: [org.acme.DummyXAResource@311931ee]
2022-09-08 22:40:36,241 INFO  [org.acm.DummyXAResource] (Periodic Recovery) Committing DummyXAResource
current audit_log entries: 1
Recovery completed successfully
^C2022-09-08 22:40:49,973 INFO  [com.arj.ats.jbossatx] (Shutdown thread) ARJUNA032010: JBossTS Recovery Service (tag: 518c8dd50a9ae8e70eb15dfe8fc764adcabef8ab) - JBoss Inc.
2022-09-08 22:40:49,973 INFO  [com.arj.ats.jbossatx] (Shutdown thread) ARJUNA032013: Starting transaction recovery manager
2022-09-08 22:40:49,984 INFO  [io.quarkus] (Shutdown thread) getting-started stopped in 0.046s

```

## Related Guides

- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): A JAX-RS implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
