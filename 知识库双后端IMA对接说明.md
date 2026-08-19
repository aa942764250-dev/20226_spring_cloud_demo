# 知识库双后端（IMA 云端 + 本地 kb_server）对接说明

> 日期：2026-08-19 ｜ 模块：springcloud-service（工作台后端 8081）
> 结论：`KnowledgeSearchClient` 已改造为可配置双后端路由，编译通过（BUILD SUCCESS）。
> 云端走腾讯 IMA 知识库 OpenAPI（WorkBuddy 资料库底层即 IMA），本地走自部署 kb_server（Project_006）。

## 一、架构与调用链

```
SelfTestQuestionGenerator / GenerationAsyncExecutor
        │  search(query, topK)            ← 调用契约不变(question/answer/source/dedupKey)
        ▼
KnowledgeSearchClient（路由，读 review.kb-mode）
        ├─ ima   → ImaKnowledgeSearcher  → POST https://ima.tencent.com/wiki/v1/knowledge/search
        ├─ local → LocalKbServerSearcher → GET  {review.kb-server-url}/search?q=..&top_k=..
        └─ both（默认）→ 双查合并，按 dedupKey 去重（IMA 在前 = IMA 优先）
```

任一后端失败只记日志返回空列表，不影响另一个后端；两个后端都未配置时整体返回空列表（行为与改造前一致）。

## 二、改动文件清单

| 文件 | 改动 |
|---|---|
| `generator/KnowledgeSearcher.java` | 新增：检索后端统一接口 |
| `generator/KnowledgeSearchUtils.java` | 新增：cleanContent / extractQuestion 共享清洗 |
| `generator/LocalKbServerSearcher.java` | 新增：原 GET 逻辑整体迁移，行为不变 |
| `generator/ImaKnowledgeSearcher.java` | 新增：IMA OpenAPI POST 调用（宽松解析，待实测收紧） |
| `generator/KnowledgeSearchClient.java` | 改写：按 kb-mode 路由 + both 去重 |
| `config/ReviewProperties.java` | 新增 kbMode / imaClientId / imaApiKey / imaKnowledgeId |
| `resources/application.yml` | 新增 kb-mode + IMA 三项（`${ENV:}` 占位） |
| `docker-compose.server.yml` | springcloud-service 增加 KB_MODE / IMA_* 环境变量 |

## 三、配置项

| 配置键 | 环境变量 | 默认 | 说明 |
|---|---|---|---|
| `review.kb-mode` | `KB_MODE` | `both` | `ima` / `local` / `both` |
| `review.kb-server-url` | - | `http://127.0.0.1:9876` | 本地 kb_server 地址 |
| `review.ima-client-id` | `IMA_CLIENT_ID` | 空 | IMA OpenAPI 凭证 |
| `review.ima-api-key` | `IMA_API_KEY` | 空 | IMA OpenAPI 凭证（只展示一次） |
| `review.ima-knowledge-id` | `IMA_KNOWLEDGE_ID` | 空 | 英语知识库在 IMA 的库 ID |
| `review.search-timeout-seconds` | - | 30 | 两个后端共用的读超时（秒） |

## 四、你要做的三步（启用云端 IMA）

1. **拿凭证**：打开 `https://ima.qq.com/agent-interface`
   → 知识库设置 → OpenAPI → 生成 Client ID + API Key。
   ⚠️ API Key 只展示一次，立即保存。
2. **上传知识库**：把 `Project_010_英语知识库/output` 的 HTML 上传到 IMA 知识库（自动向量化），记下 `knowledge_id`。
   若 IMA 不支持直接传 HTML：先转 PDF（浏览器打印为 PDF）再传。
3. **注入环境变量后启动**：
   - 本地：`export IMA_CLIENT_ID=xxx IMA_API_KEY=xxx IMA_KNOWLEDGE_ID=xxx`（Windows PowerShell 用 `$env:IMA_CLIENT_ID="xxx"`）
   - 远程 docker compose：在宿主机 export，或项目根建 `.env` 文件（compose 自动读取）：
     ```
     IMA_CLIENT_ID=xxx
     IMA_API_KEY=xxx
     IMA_KNOWLEDGE_ID=xxx
     KB_MODE=both
     ```

## 五、IMA API 要点（已查证）

- 检索接口：`POST https://ima.tencent.com/wiki/v1/knowledge/search`
- 认证**不是**标准 Authorization，而是两个自定义头：
  - `ima-openapi-clientid: <CLIENT_ID>`
  - `ima-openapi-apikey: <API_KEY>`
- `Content-Type` 必须显式 `application/json`（缺了被拒）
- ⚠️ 路径前缀必须是 `/wiki/v1/`（写成 `/api/v1/` 会 401）
- 请求体：`{"knowledge_id":"...","query":"...","top_k":5}`
- 返回：匹配片段列表 + 相似度分数

## 六、待实测（拿到 knowledge_id 后做一次）

```bash
curl -X POST 'https://ima.tencent.com/wiki/v1/knowledge/search' \
  -H 'ima-openapi-clientid: YOUR_CLIENT_ID' \
  -H 'ima-openapi-apikey: YOUR_API_KEY' \
  -H 'Content-Type: application/json' \
  -d '{"knowledge_id":"YOUR_KB_ID","query":"现在完成时","top_k":3}'
```

1. 把返回的 JSON 原样发给 AI 助手 → 收紧 `ImaKnowledgeSearcher.resolveResultArray()` 的宽松解析为精确字段名。
2. 若 search 只返回片段不带完整正文：需再调 `get_media_info`（签名 URL）二次取正文，届时在 ImaKnowledgeSearcher 加一步。
3. 验证模式切换：`KB_MODE=ima`（只云端）/ `KB_MODE=local`（只本地）/ 不设（both 双查）。

## 七、编译与验证记录

- 2026-08-19：`mvn -pl springcloud-service -am compile` → **BUILD SUCCESS**（79 源文件）。
- 本机 git bash 下直接跑 `mvn` 会报
  `找不到或无法加载主类 org.codehaus.plexus.classworlds.launcher.Launcher`
  （MAVEN_HOME 是 Windows 反斜杠路径所致），改用 `mvn.cmd` 即可。
