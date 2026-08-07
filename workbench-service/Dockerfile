# 基础镜像走 DaoCloud 镜像源（本机 docker.io 不通，Docker Desktop 无代理）
FROM docker.m.daocloud.io/library/eclipse-temurin:8-jre
LABEL app="workbench-service"
WORKDIR /app
COPY target/*.jar app.jar
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8083
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
