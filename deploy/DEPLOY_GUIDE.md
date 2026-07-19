# 华为云 CCE 部署指南

## 前置条件

- 华为云账号（已实名认证）
- 已安装 Docker、kubectl 命令行工具

---

## 第一步：创建华为云资源

### 1.1 创建 CCE 集群

1. 登录 [华为云 CCE 控制台](https://console.huaweicloud.com/cce/)
2. 点击 **创建集群**
3. 配置建议：
   - 集群类型：CCE Standard
   - 版本：v1.27+（推荐最新稳定版）
   - 区域：华北-北京四（cn-north-4）
   - 集群规模：50节点
   - 网络模型：VPC 网络
4. 创建节点池：
   - 节点规格：2核4G 及以上（推荐 c6s.2xlarge.2）
   - 节点数量：2
   - 操作系统：EulerOS 2.9
5. 等待集群创建完成（约 10-15 分钟）

### 1.2 创建 SWR 组织

1. 登录 [华为云 SWR 控制台](https://console.huaweicloud.com/swr/)
2. 点击 **组织管理** → **创建组织**
3. 组织名称：`springcloud-demo`

### 1.3 获取 kubeconfig

1. 在 CCE 集群详情页，点击 **kubectl**
2. 选择 **内网/公网** 访问方式
3. 下载 kubeconfig 文件
4. 将 kubeconfig 放到本地 `~/.kube/config`

---

## 第二步：构建并推送镜像

```bash
# 在项目根目录执行
chmod +x deploy/build-and-push.sh
./deploy/build-and-push.sh
```

脚本会依次执行：
1. Maven 构建打包
2. Docker 构建镜像
3. 登录华为云 SWR
4. 推送镜像到 SWR

> 也可在 SWR 控制台获取临时登录命令，手动执行 docker login

---

## 第三步：部署到 CCE

```bash
chmod +x deploy/deploy-to-cce.sh
./deploy/deploy-to-cce.sh
```

脚本会按顺序部署：
1. Namespace + ConfigMap
2. MySQL（等待就绪）
3. Nacos（等待就绪）
4. springcloud-service（等待就绪）
5. springcloud-gateway（等待就绪）

---

## 第四步：验证

```bash
# 查看所有资源
kubectl get all -n springcloud-demo

# 获取网关外部 IP
kubectl get svc springcloud-gateway -n springcloud-demo

# 测试接口（替换为实际 EIP）
curl http://<EXTERNAL-IP>:8080/api/user/list
```

---

## 生产环境建议

| 项目 | 当前配置 | 生产建议 |
|------|---------|---------|
| MySQL | emptyDir | 使用华为云 RDS for MySQL |
| Nacos | 内嵌 Derby | 使用 CSE 注册配置中心 |
| MySQL 密码 | 明文环境变量 | 使用 CCE 密钥管理（Secret） |
| 数据持久化 | emptyDir | 使用云硬盘 EVS（PV/PVC） |
| 副本数 | 1 | 业务服务 ≥ 2 |
| 网关暴露 | LoadBalancer | 配合 WAF + 域名 + HTTPS |

---

## 常用运维命令

```bash
# 查看 Pod 日志
kubectl logs -f <pod-name> -n springcloud-demo

# 进入容器
kubectl exec -it <pod-name> -n springcloud-demo -- /bin/sh

# 重启服务
kubectl rollout restart deployment/<name> -n springcloud-demo

# 删除所有部署
kubectl delete -f deploy/k8s/
```