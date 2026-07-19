FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY springcloud-gateway/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]