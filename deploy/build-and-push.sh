#!/bin/bash

REGION="cn-north-4"
ORG="springcloud-demo"
SWR_REGISTRY="swr.${REGION}.myhuaweicloud.com"

SERVICES=("springcloud-gateway" "springcloud-service")
TAG="1.0.0"

echo "========================================"
echo " 步骤1: Maven 构建"
echo "========================================"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Maven 构建失败!"
    exit 1
fi

echo "========================================"
echo " 步骤2: Docker 构建镜像"
echo "========================================"
for svc in "${SERVICES[@]}"; do
    echo "构建镜像: ${svc}..."
    docker build -f ${svc}/Dockerfile -t ${SWR_REGISTRY}/${ORG}/${svc}:${TAG} .
    if [ $? -ne 0 ]; then
        echo "构建 ${svc} 镜像失败!"
        exit 1
    fi
done

echo "========================================"
echo " 步骤3: 登录华为云 SWR"
echo "========================================"
echo "请输入华为云 IAM 用户名:"
read IAM_USER
echo "请输入华为云 IAM 密码:"
read -s IAM_PASS

docker login -u "${IAM_USER}@${REGION}" -p "$(echo -n ${IAM_PASS} | base64)" ${SWR_REGISTRY}
if [ $? -ne 0 ]; then
    echo "SWR 登录失败! 请检查凭据"
    exit 1
fi

echo "========================================"
echo " 步骤4: 推送镜像到 SWR"
echo "========================================"
for svc in "${SERVICES[@]}"; do
    echo "推送镜像: ${svc}..."
    docker push ${SWR_REGISTRY}/${ORG}/${svc}:${TAG}
    if [ $? -ne 0 ]; then
        echo "推送 ${svc} 镜像失败!"
        exit 1
    fi
done

echo "========================================"
echo " 所有镜像推送完成!"
echo "========================================"
for svc in "${SERVICES[@]}"; do
    echo "  ${SWR_REGISTRY}/${ORG}/${svc}:${TAG}"
done