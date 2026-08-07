# springcloud-demo Python 脚本集中管理

## 目录结构

```
scripts/
├── core/           # 核心脚本（被 Java/Spring Boot 直接引用）
│   ├── kb.py       # 本地知识库检索（ProcessBuilder 调用，application.yml 配置）
│   ├── ingest.py   # 知识库文档索引导入（PDF/DOCX → Qdrant）
│   ├── knowledge.py # 知识库核心模块（切分、向量检索）
│   ├── md2pdf.py   # Markdown 转 PDF
│   └── knowledge_mcp_server.py  # 本地知识库 MCP 服务器
│
├── db/             # 数据库操作脚本
│   ├── run_ddl.py  # 自测系统 DDL（扩展 review_item + 新建 3 表）
│   ├── init_review_tables.py    # 复习系统建表
│   ├── generate_selftest.py     # 生成自测题目写入数据库
│   ├── check_review_data.py     # 检查 review 数据状态
│   └── cleanup_wrong_date.py    # 清理错误日期数据
│
├── generate/       # 数据生成脚本
│   ├── insert_module.py         # 按模块检索知识库插入 review_item
│   ├── direct_generate.py       # 直接生成复习数据写入数据库
│   ├── gen_pdf.py               # 自测清单 Markdown → PDF
│   └── generate_review_md.py    # 从数据库生成复习 Markdown
│
├── server/         # 服务启停脚本
│   ├── start_bg.py              # 后台启动 Spring Boot（Maven）
│   ├── start_boot.py            # 前台启动 Spring Boot（Maven）
│   ├── start_server.py          # 后台启动 Spring Boot JAR
│   ├── start_and_generate.py    # 启动 JAR 后自动调用生成接口
│   ├── call_generate.py         # HTTP 调用 /api/review/generate
│   ├── call_gen_bg.py           # HTTP 调用生成接口（后台）
│   └── start_vite.py            # 后台启动 Vite 前端开发服务器
│
└── deploy/         # 部署脚本
    ├── deploy_frontend.py       # SSH/SFTP 部署前端 dist 到远程服务器
    ├── create_test_tables.py    # SSH 远程建表
    └── query_core.py            # 查询 alliance_showcase 核心成员
```

## Java 引用关系

```
application.yml (kb-script-path)
  → ReviewProperties (@ConfigurationProperties)
    → KnowledgeSearchClient (ProcessBuilder)
      → scripts/core/kb.py search <query> --top-k N
        → 返回 JSON: {query, results: [{title, content, source, score}]}
```

## 注意

- `core/` 下的脚本来自 `Project_006_LocalKnowledgeMCP`，是 Java 后端唯一直接依赖的 Python 脚本
- 其他脚本为运维/工具脚本，可独立运行
- 修改 `core/kb.py` 后需重启 Spring Boot 生效