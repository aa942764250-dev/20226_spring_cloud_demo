"""Local Knowledge MCP 便捷检索工具

用法:
  python kb.py search "查询内容" [--top-k 5]
  python kb.py list
  python kb.py chunk <chunk_id>
"""
import argparse
import json
import os
import sys

# 确保离线模式，避免 Hugging Face 联网
os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.pop("ALL_PROXY", None)
os.environ.pop("HTTPS_PROXY", None)
os.environ.pop("HTTP_PROXY", None)

import knowledge


def cmd_search(args):
    results = knowledge.search(args.query, top_k=args.top_k)
    print(json.dumps({"query": args.query, "results": results}, ensure_ascii=False, indent=2))


def cmd_list(args):
    sources = knowledge.list_sources()
    print(json.dumps({"total": len(sources), "documents": sources}, ensure_ascii=False, indent=2))


def cmd_chunk(args):
    result = knowledge.get_chunk(args.chunk_id)
    print(json.dumps(result, ensure_ascii=False, indent=2))


def main():
    parser = argparse.ArgumentParser(description="Local Knowledge 便捷检索")
    sub = parser.add_subparsers(dest="command", required=True)

    p_search = sub.add_parser("search", help="语义+关键词混合检索")
    p_search.add_argument("query", help="检索内容")
    p_search.add_argument("--top-k", type=int, default=5, help="返回结果数 (1-10)")
    p_search.set_defaults(func=cmd_search)

    p_list = sub.add_parser("list", help="列出已索引文档")
    p_list.set_defaults(func=cmd_list)

    p_chunk = sub.add_parser("chunk", help="读取完整片段")
    p_chunk.add_argument("chunk_id", help="片段 ID")
    p_chunk.set_defaults(func=cmd_chunk)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()