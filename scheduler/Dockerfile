FROM maven:3.8.6-openjdk-18
RUN  ["git", "clone", "https://user:pass@github.com/BSSG-ru/root/lottabyte.git", "--branch", "github", "--single-branch"]
RUN  ["sh", "-c", "sed -i 's@spring.datasource.url=jdbc:postgresql:\/\/.*\/glossaryDB@spring.datasource.url=jdbc:postgresql:\/\/cluster1-pgbouncer.pgo:5432\/glossaryDB@g' ./lottabyte/scheduler/src/main/resources/application.properties"]
RUN  ["sh", "-c", "sed -i 's@lottabyte.api.url=http:\/\/127.0.0.1:8080@lottabyte.api.url=https:\/\/localhost@g' ./lottabyte/scheduler/src/main/resources/application.properties"]
RUN  ["sh", "-c", "sed -i 's@server.port=.*@server.port=8080@g' ./lottabyte/scheduler/src/main/resources/application.properties"]
RUN  ["sh", "-c", "cd lottabyte && mvn clean package -pl shared,scheduler"]
EXPOSE 8080
CMD  ["java", "-Dspring.config.location=classpath:file:/config/application.properties", "-jar", "lottabyte/scheduler/target/scheduler-1.0-SNAPSHOT.jar"]
