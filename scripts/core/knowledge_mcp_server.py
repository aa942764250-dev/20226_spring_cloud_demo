import asyncio
import json

from mcp.server.fastmcp import FastMCP

import knowledge


mcp = FastMCP("local-knowledge")


@mcp.tool()
async def search_knowledge(query: str, top_k: int = 5) -> str:
    """Search local knowledge when a question concerns indexed local documents."""
    results = await asyncio.to_thread(knowledge.search, query, top_k)
    return json.dumps({"query": query, "results": results}, ensure_ascii=False)


@mcp.tool()
async def get_document_chunk(chunk_id: str) -> str:
    """Read one full source chunk returned by search_knowledge."""
    result = await asyncio.to_thread(knowledge.get_chunk, chunk_id)
    return json.dumps(result, ensure_ascii=False)


@mcp.tool()
async def list_knowledge_documents() -> str:
    """List indexed local documents without reading their full contents."""
    sources = await asyncio.to_thread(knowledge.list_sources)
    return json.dumps({"documents": sources}, ensure_ascii=False)


if __name__ == "__main__":
    mcp.run()
