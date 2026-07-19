$REGION = "cn-north-4"
$ORG = "springcloud-demo"
$SWR_REGISTRY = "swr.$REGION.myhuaweicloud.com"
$TAG = "1.0.0"
$SERVICES = @("springcloud-gateway", "springcloud-service")
$INFRA_IMAGES = @(
    @{ Name = "mysql"; Source = "mysql:8.0"; Tag = "8.0" },
    @{ Name = "nacos"; Source = "nacos/nacos-server:v2.2.3"; Tag = "v2.2.3" }
)

Write-Host "========================================"
Write-Host " 步骤1: Maven 构建"
Write-Host "========================================"
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven 构建失败!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================"
Write-Host " 步骤2: 拉取并标记基础镜像 (MySQL, Nacos)"
Write-Host "========================================"
foreach ($img in $INFRA_IMAGES) {
    Write-Host "拉取镜像: $($img.Source) ..."
    docker pull $($img.Source)
    if ($LASTEXITCODE -ne 0) {
        Write-Host "拉取 $($img.Source) 失败!" -ForegroundColor Red
        exit 1
    }
    docker tag $($img.Source) "${SWR_REGISTRY}/${ORG}/$($img.Name):$($img.Tag)"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "标记 $($img.Name) 失败!" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host " 步骤3: Docker 构建业务镜像"
Write-Host "========================================"
foreach ($svc in $SERVICES) {
    Write-Host "构建镜像: $svc ..."
    docker build -f "$svc/Dockerfile" -t "${SWR_REGISTRY}/${ORG}/${svc}:${TAG}" .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "构建 ${svc} 镜像失败!" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host " 步骤4: 登录华为云 SWR 制品仓库"
Write-Host "========================================"
Write-Host "请输入华为云 IAM 用户名: " -NoNewline
$IAM_USER = Read-Host
Write-Host "请输入华为云 IAM 密码: " -NoNewline
$IAM_PASS = Read-Host -AsSecureString
$IAM_PASS_Plain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($IAM_PASS))
$IAM_PASS_B64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($IAM_PASS_Plain))

docker login -u "${IAM_USER}@${REGION}" -p $IAM_PASS_B64 $SWR_REGISTRY
if ($LASTEXITCODE -ne 0) {
    Write-Host "SWR 登录失败! 请检查凭据" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================"
Write-Host " 步骤5: 推送基础镜像到 SWR 制品仓库"
Write-Host "========================================"
foreach ($img in $INFRA_IMAGES) {
    Write-Host "推送镜像: $($img.Name) ..."
    docker push "${SWR_REGISTRY}/${ORG}/$($img.Name):$($img.Tag)"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "推送 $($img.Name) 镜像失败!" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host " 步骤6: 推送业务镜像到 SWR 制品仓库"
Write-Host "========================================"
foreach ($svc in $SERVICES) {
    Write-Host "推送镜像: $svc ..."
    docker push "${SWR_REGISTRY}/${ORG}/${svc}:${TAG}"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "推送 ${svc} 镜像失败!" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host " 所有镜像推送完成!" -ForegroundColor Green
Write-Host "========================================"
Write-Host ""
Write-Host "基础镜像:"
foreach ($img in $INFRA_IMAGES) {
    Write-Host "  ${SWR_REGISTRY}/${ORG}/$($img.Name):$($img.Tag)"
}
Write-Host ""
Write-Host "业务镜像:"
foreach ($svc in $SERVICES) {
    Write-Host "  ${SWR_REGISTRY}/${ORG}/${svc}:${TAG}"
}
Write-Host ""
Write-Host "可在华为云 SWR 控制台查看:"
Write-Host "  https://console.huaweicloud.com/swr/"