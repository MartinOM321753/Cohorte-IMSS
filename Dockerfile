FROM openjdk:21-ea-1-jdk-slim
ARG JAR_FILE=target/cohorte_test-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} cohorte_test.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "cohorte_test.jar"]