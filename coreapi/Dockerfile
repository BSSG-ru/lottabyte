FROM maven:3.8.6-openjdk-18
RUN  ["git", "clone", "https://user:pass@github.com/BSSG-ru/root/lottabyte.git", "--branch", "github", "--single-branch"]
RUN  ["sh", "-c", "sed -i 's@spring.datasource.url=jdbc:postgresql:\/\/.*\/glossaryDB@spring.datasource.url=jdbc:postgresql:\/\/cluster1-pgbouncer.pgo:5432\/prodGlossaryDB@g' ./lottabyte/coreapi/src/main/resources/application.properties"]
RUN  ["sh", "-c", "cd lottabyte && mvn clean package -pl shared,coreapi"]
EXPOSE 8080
ENTRYPOINT  ["java", "-Dspring.config.location=classpath:file:/config/application.properties", "-jar", "/lottabyte/coreapi/target/coreapi-1.0-SNAPSHOT.jar"]
