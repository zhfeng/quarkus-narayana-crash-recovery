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
   CREATE DATABASE tslog CHARACTER SET utf8mb4;
   CREATE USER 'admin'@'%' IDENTIFIED WITH mysql_native_password BY 'admin';
   GRANT ALL ON testdb.* TO 'admin'@'%';
   GRANT XA_RECOVER_ADMIN on *.* to 'admin'@'%';
   GRANT ALL ON tslog.* TO 'admin'@'%';
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

## Test commit message
```shell script
java -jar target/quarkus-app/quarkus-run.jar
curl -X POST http://localhost:8080/hello -d "commit"
```

And you will see:
```
2022-09-13 22:00:40,993 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032010: JBossTS Recovery Service (tag: 518c8dd50a9ae8e70eb15dfe8fc764adcabef8ab) - JBoss Inc.
2022-09-13 22:00:41,023 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032013: Starting transaction recovery manager
2022-09-13 22:00:41,091 INFO  [org.acm.DummyXAResourceRecovery] (main) register DummyXAResourceRecovery
2022-09-13 22:00:41,151 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.12.1.Final) started in 0.533s. Listening on: http://0.0.0.0:8080
2022-09-13 22:00:41,153 INFO  [io.quarkus] (main) Profile prod activated. 
2022-09-13 22:00:41,153 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, jdbc-mysql, narayana-jta, resteasy-reactive, smallrye-context-propagation, vertx]
2022-09-13 22:00:46,225 INFO  [org.acm.DummyXAResource] (executor-thread-0) Preparing DummyXAResource
2022-09-13 22:00:46,264 INFO  [org.acm.DummyXAResource] (executor-thread-0) Committing DummyXAResource
```

Check the database:
```
mysql> select * from audit_log;

+-----+---------+
| id  | message |
+-----+---------+
|    1| commit  |
+-----+---------+
```

## Test rollback message
```
java -jar target/quarkus-app/quarkus-run.jar
curl -X POST http://localhost:8080/hello -d "rollback"
```

And you will see:
```
2022-09-13 22:01:04,552 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032010: JBossTS Recovery Service (tag: 518c8dd50a9ae8e70eb15dfe8fc764adcabef8ab) - JBoss Inc.
2022-09-13 22:01:04,582 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032013: Starting transaction recovery manager
2022-09-13 22:01:04,640 INFO  [org.acm.DummyXAResourceRecovery] (main) register DummyXAResourceRecovery
2022-09-13 22:01:04,697 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.12.1.Final) started in 0.520s. Listening on: http://0.0.0.0:8080
2022-09-13 22:01:04,698 INFO  [io.quarkus] (main) Profile prod activated. 
2022-09-13 22:01:04,698 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, jdbc-mysql, narayana-jta, resteasy-reactive, smallrye-context-propagation, vertx]
2022-09-13 22:01:40,071 INFO  [org.acm.DummyXAResource] (executor-thread-0) Rolling back DummyXAResource
```

And no new messge in the database


## Test crash recovery
### First run your application and send a message to make it crash
```shell script
java -jar target/quarkus-app/quarkus-run.jar
curl -X POST http://localhost:8080/hello -d "crash"
```

And your application will crash with the following error:
```
2022-09-13 22:00:40,993 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032010: JBossTS Recovery Service (tag: 518c8dd50a9ae8e70eb15dfe8fc764adcabef8ab) - JBoss Inc.
2022-09-13 22:00:41,023 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032013: Starting transaction recovery manager
2022-09-13 22:00:41,091 INFO  [org.acm.DummyXAResourceRecovery] (main) register DummyXAResourceRecovery
2022-09-13 22:00:41,151 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.12.1.Final) started in 0.533s. Listening on: http://0.0.0.0:8080
2022-09-13 22:00:41,153 INFO  [io.quarkus] (main) Profile prod activated. 
2022-09-13 22:00:41,153 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, jdbc-mysql, narayana-jta, resteasy-reactive, smallrye-context-propagation, vertx]
2022-09-13 22:00:57,969 INFO  [org.acm.DummyXAResource] (executor-thread-0) Preparing DummyXAResource
2022-09-13 22:00:57,999 INFO  [org.acm.DummyXAResource] (executor-thread-0) Committing DummyXAResource
2022-09-13 22:00:57,999 INFO  [org.acm.DummyXAResource] (executor-thread-0) Crashing the system
```

And check the database:
```shell script
mysql> xa recover;

+----------+--------------+--------------+-------------------------------------------------------------------------+
| formatID | gtrid_length | bqual_length | data                                                                    |
+----------+--------------+--------------+-------------------------------------------------------------------------+
|   131077 |           35 |           36 |           ����e  ��c �~   quarkus          ����e  ��c �~                | 
+----------+--------------+--------------+-------------------------------------------------------------------------+

```

### Restart your application and wait for the recovery happening
```shell script
java -jar target/quarkus-app/quarkus-run.jar
# you need to wait about 10 sec
```

And you will see the following messages:
```
2022-09-13 21:55:59,310 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032010: JBossTS Recovery Service (tag: 518c8dd50a9ae8e70eb15dfe8fc764adcabef8ab) - JBoss Inc.
2022-09-13 21:55:59,337 INFO  [com.arj.ats.jbossatx] (main) ARJUNA032013: Starting transaction recovery manager
2022-09-13 21:55:59,400 INFO  [org.acm.DummyXAResourceRecovery] (main) register DummyXAResourceRecovery
2022-09-13 21:55:59,460 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.12.1.Final) started in 0.519s. Listening on: http://0.0.0.0:8080
2022-09-13 21:55:59,462 INFO  [io.quarkus] (main) Profile prod activated. 
2022-09-13 21:55:59,462 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, jdbc-mysql, narayana-jta, resteasy-reactive, smallrye-context-propagation, vertx]
2022-09-13 21:56:09,491 INFO  [org.acm.DummyXAResourceRecovery] (Periodic Recovery) DummyXAResourceRecovery returning list of resources: [org.acme.DummyXAResource@914c4c2]
2022-09-13 21:56:09,503 INFO  [org.acm.DummyXAResource] (Periodic Recovery) Committing DummyXAResource
```

check the database;
```shell script
mysql> xa recover;
Empty set (0.001 sec)

mysql> select * from audit_log;

+-----+---------+
| id  | message |
+-----+---------+
|    1| commit  |
|    3| crash   |
+-----+---------+
```

## Related Guides

- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): A JAX-RS implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
