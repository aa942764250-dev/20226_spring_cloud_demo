# 本地自包含联调运行手册（local profile）

> 用途：不依赖任何外部服务（无需远程 MySQL / Nacos），在本机一条命令启动整套微服务并跑通
> 「工作台(workbench) → 登录拿 token → AI 教师(ai-teacher)」联动。
> 远程 `dev` profile 的配置**未改动**，生产/远程部署仍走原 Nacos + 远程库。

## 一、原理

为每个服务新增 `local` profile：
- **数据库**：用 H2 内存库（`MODE=MySQL` 兼容模式），启动自动执行 `db/*.sql` 建表 + 种子数据。
- **服务发现**：关闭 Nacos（`spring.cloud.nacos.discovery.enabled=false`）。
- **网关路由**：由 `lb://` 改为直连本地实例（`http://127.0.0.1:8083` / `:8084`）。
- 网关 JWT 拦截、workbench 登录、token 注入、ai-teacher 各接口全部复用原有逻辑。

## 二、启动（3 个服务）

```bat
set JAVA="D:/Program Files/jdk1.8/bin/java.exe"
set PROJ=D:/Workspace/springcloud-demo

start "gateway"  %JAVA% -jar %PROJ%/springcloud-gateway/target/springcloud-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active=local --server.port=8080
start "workbench" %JAVA% -jar %PROJ%/workbench-service/target/workbench-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=local --server.port=8083
start "ai-teacher" %JAVA% -jar %PROJ%/ai-teacher-service/target/ai-teacher-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=local --server.port=8084
```

> 端口固定用 `--server.port=`，以覆盖容器/沙箱可能注入的 `SERVER_PORT=0`（随机端口）。
> H2 内存库在进程退出即清空，每次启动都是干净种子数据。

## 三、联调验证（已实测全绿）

| 场景 | 请求 | 期望 |
|------|------|------|
| 网关 JWT 拦截 | `GET /api/ai-teacher/dashboard/overview`（无 token） | **401** |
| 登录拿 token | `POST /api/wb/auth/login` `{username:admin,password:123456}` | **200** + token + 菜单（含「AI教师」→/ai-teacher） |
| 看板 | `GET /api/ai-teacher/dashboard/overview`（带 token） | 200，含 stats/pendingRecords/weeklyTrend |
| 分析 | `GET /api/ai-teacher/analysis/overview`（带 token） | 200，含等级/课程分布、能力对比、6 月频次 |
| 学生列表 | `GET /api/ai-teacher/student/list?page=1&size=5` | 200 |
| 学生详情 | `GET /api/ai-teacher/student/8` | 200（含能力画像/趋势） |
| 报告生成（占位） | `POST /api/ai-teacher/report/generate` | 200（**不调 Gemini**，返回报告 id） |

账号：**admin / 123456**（BCrypt，种子数据见 `workbench-service/.../db/data-workbench.sql`）。

## 四、种子数据

- `workbench`：wb_user / wb_role / wb_user_role / wb_menu / wb_role_menu（admin + ADMIN 角色 + 菜单）。
- `ai-teacher`：8 名学生、学习记录（含 2026-07-29~08-04 区间，供周报/趋势）、能力评分（六维）、周/月报 Prompt 模板、1 条示例报告。
  见 `ai-teacher-service/src/main/resources/db/data-ai-teacher.sql`。

## 五、踩坑记录（local 联调时解决）

1. **H2 兼容模式**：JDBC URL 必须带 `;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`，
   否则 MySQL 建表 SQL（反引号 / `ENGINE=` / `KEY` / `LONGTEXT` / `AUTO_INCREMENT`）会报错。
2. **`value` 是 H2 保留字**：`count(*) as value` 必须写成 `` count(*) as `value` ``，否则报 "expected identifier"。
3. **`DATE_FORMAT` 在 H2 不存在**（即使 `MODE=MySQL`）：`AnalysisServiceImpl.frequencyTrend` 改为在 Java 端按年月分桶。
4. **打包锁**：`spring-boot-maven-plugin:repackage` 重命名 jar 时，若有正在运行的实例持有 jar 文件锁会失败，
   需先 `taskkill /F /IM java.exe` 再重新 `package`。
5. **终端中文乱码**：Windows 控制台 GBK 导致 JSON 中文显示为乱码，属显示问题，接口返回本身是 UTF-8 正常。

## 六、若要用自己的本地 MySQL + Nacos（非 H2）

保持 `dev` profile 不变，把各服务 `application-dev.yml` 的 datasource 指向本机 MySQL（建库 `springcloud_demo`，
执行 `db/init_ai_teacher_tables.sql` + `data-ai-teacher.sql` 和 wb_user 体系），Nacos 指向本机 `8848` 即可，
启动时不加 `--spring.profiles.active=local`（默认走 `dev`）。
