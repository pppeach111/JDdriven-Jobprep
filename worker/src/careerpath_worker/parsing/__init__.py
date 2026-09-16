"""文档解析能力（JD / 简历结构化）。

状态：接口占位。具体实现属于 CP-003，此处只冻结边界与契约，不提供假实现。

契约约束（shared_docs/14-contracts-and-schemas.md 第 4 节、16-model-prompt-contracts.md）：
    - 输出必须先通过 Schema 校验，再进入业务规则；
    - 原文没有的字段返回 null 或进入 unknowns，不得猜测；
    - 推断内容必须显式标记，且不得升级为硬门槛；
    - 每条结果保留 sourceQuote 与 sourceLocator 以便回溯。
"""

from __future__ import annotations

from typing import Any, Protocol, runtime_checkable

__all__ = ["JdExtractor", "NotImplementedExtractor"]


@runtime_checkable
class JdExtractor(Protocol):
    """JD 结构化提取接口。"""

    async def extract(self, raw_text: str, source_id: str) -> dict[str, Any]:
        """把 JD 原文转换为契约第 4 节定义的结构化结果。"""
        ...


class NotImplementedExtractor:
    """占位实现：任何调用都明确失败，避免上游误以为功能已可用。"""

    async def extract(self, raw_text: str, source_id: str) -> dict[str, Any]:
        raise NotImplementedError(
            "JD 结构化提取尚未实现（计划于 CP-003 交付）。"
            "在此之前请使用 Spring Boot 侧的规则式解析器。"
        )