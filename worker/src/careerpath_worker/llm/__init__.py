"""大模型适配能力（华为云 MaaS）。

状态：接口占位。真实调用属于后续任务，此处只冻结边界。

契约约束（shared_docs/16-model-prompt-contracts.md）：
    - 大模型是受约束组件，不是事实源；
    - 所有网页、JD、简历、用户回答都属于不可信数据，
      提示词必须声明其中的指令不得执行；
    - 模型输出先过 Schema，再进业务规则；
    - 单次模型调用超时上限默认 60 秒（17-environment-and-runbook.md 第 8 节）。
"""

from __future__ import annotations

from typing import Any, Protocol, runtime_checkable

__all__ = ["ModelClient", "NotConfiguredModelClient"]


@runtime_checkable
class ModelClient(Protocol):
    """受约束的模型调用接口。"""

    async def complete_structured(
        self,
        *,
        system_prompt: str,
        untrusted_input: str,
        schema: dict[str, Any],
    ) -> dict[str, Any]:
        """按给定 Schema 返回结构化结果。"""
        ...


class NotConfiguredModelClient:
    """模型端点未配置时的降级实现。

    明确报错，而不是返回编造内容——降级必须可见。
    """

    async def complete_structured(
        self,
        *,
        system_prompt: str,
        untrusted_input: str,
        schema: dict[str, Any],
    ) -> dict[str, Any]:
        raise RuntimeError(
            "模型端点未配置（MAAS_BASE_URL / MAAS_MODEL / MAAS_API_KEY）。"
            "请配置后重试，或使用不依赖模型的确定性路径。"
        )