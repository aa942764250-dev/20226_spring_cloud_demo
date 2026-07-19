#!/bin/bash

echo "========================================"
echo " 部署 Spring Cloud Demo 到华为云 CCE"
echo "========================================"

if ! command -v kubectl &> /dev/null; then
    echo "错误: kubectl 未安装"
    echo "请先安装 kubectl: https://kubernetes.io/docs/tasks/tools/"
    exit 1
fi

if ! kubectl cluster-info &> /dev/null; then
    echo "错误: kubectl 未连接到 CCE 集群"
    echo "请在华为云 CCE 控制台获取 kubeconfig 并配置:"
    echo "  https://console.huaweicloud.com/cce/"
    exit 1
fi

K8S_DIR="$(dirname "$0")/k8s"

echo "========================================"
echo " 步骤1: 创建 Namespace 和 ConfigMap"
echo "========================================"
kubectl apply -f ${K8S_DIR}/00-namespace.yml
kubectl apply -f ${K8S_DIR}/00-configmap.yml

echo "========================================"
echo " 步骤2: 部署 MySQL"
echo "========================================"
kubectl apply -f ${K8S_DIR}/01-mysql.yml
echo "等待 MySQL 就绪..."
kubectl wait --for=condition=available deployment/mysql -n springcloud-demo --timeout=180s

echo "========================================"
echo " 步骤3: 部署 Nacos"
echo "========================================"
kubectl apply -f ${K8S_DIR}/02-nacos.yml
echo "等待 Nacos 就绪..."
kubectl wait --for=condition=available deployment/nacos -n springcloud-demo --timeout=180s

echo "========================================"
echo " 步骤4: 部署 springcloud-service"
echo "========================================"
kubectl apply -f ${K8S_DIR}/03-service.yml
echo "等待 springcloud-service 就绪..."
kubectl wait --for=condition=available deployment/springcloud-service -n springcloud-demo --timeout=180s

echo "========================================"
echo " 步骤5: 部署 springcloud-gateway"
echo "========================================"
kubectl apply -f ${K8S_DIR}/04-gateway.yml
echo "等待 springcloud-gateway 就绪..."
kubectl wait --for=condition=available deployment/springcloud-gateway -n springcloud-demo --timeout=180s

echo "========================================"
echo " 部署完成!"
echo "========================================"
echo ""
echo "查看服务状态:"
echo "  kubectl get all -n springcloud-demo"
echo ""
echo "获取网关外部访问地址:"
echo "  kubectl get svc springcloud-gateway -n springcloud-demo"
echo ""
echo "测试接口:"
echo "  curl http://<EXTERNAL-IP>:8080/api/user/list"