# 基础镜像走 DaoCloud 镜像源（本机 docker.io 不通，Docker Desktop 无代理）
FROM docker.m.daocloud.io/library/eclipse-temurin:8-jre
LABEL app="ai-teacher-service"
WORKDIR /app
COPY target/*.jar app.jar
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
