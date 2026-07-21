FROM eclipse-temurin:8-jre
LABEL app="springcloud-service"
WORKDIR /app
COPY springcloud-service-1.0.0-SNAPSHOT.jar app.jar
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar app.jar --spring.profiles.active=master"]
