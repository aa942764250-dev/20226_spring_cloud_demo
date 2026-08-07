# springcloud-service (工作台后端, 8081) — 本地 docker 用 dev profile 连服务器 Nacos+MySQL
# 基础镜像走 DaoCloud 源（docker.io 本机不通）
FROM docker.m.daocloud.io/library/eclipse-temurin:8-jre
WORKDIR /app
COPY target/springcloud-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev}"]
