"""Worker 内部健康接口。

边界（shared_docs/17-environment-and-runbook.md 第 9 节）：
    Python Worker 通过异步任务或内部接口提供采集、解析和模型适配能力，
    不对外承担主 API 契约。

健康语义（同文档第 7 节）：
    "Worker 至少提供当前任务数、最近失败数和最后成功时间；
     不能以进程存活代替任务健康。"
    "模型端点不可用时，ready 是否失败要按环境配置：
     本地可降级，演示环境必须明确显示降级状态。"
"""

from __future__ import annotations

from typing import Any

from fastapi import FastAPI

from careerpath_worker import __version__
from careerpath_worker.config import Settings, get_settings
from careerpath_worker.tasks.registry import TaskRegistry, default_registry

#: 这些环境下模型未配置属于降级，不算未就绪。
DEGRADABLE_ENVS = frozenset({"local", "dev", "test"})


def _dependency_states(settings: Settings) -> dict[str, str]:
    return {
        "database": "configured" if settings.database_url else "not_configured",
        "redis": "configured" if settings.redis_url else "not_configured",
        "model": "configured" if settings.model_configured else "not_configured",
    }


def _health_payload(settings: Settings, registry: TaskRegistry) -> dict[str, Any]:
    snapshot = registry.snapshot()
    return {
        "status": "ok",
        "version": __version__,
        "appEnv": settings.app_env,
        "tasks": {
            "running": snapshot.running_tasks,
            "recentFailures": snapshot.recent_failures,
            "lastSuccessAt": (
                snapshot.last_success_at.isoformat() if snapshot.last_success_at else None
            ),
        },
        "dependencies": _dependency_states(settings),
    }


def create_app(
    settings: Settings | None = None,
    registry: TaskRegistry | None = None,
) -> FastAPI:
    """构造 FastAPI 应用。允许注入配置与注册表，便于测试。"""
    active_settings = settings or get_settings()
    active_registry = registry or default_registry

    app = FastAPI(
        title="CareerPath Worker",
        version=__version__,
        description="采集、文档解析与模型适配的内部 Worker（非主业务 API）",
    )

    @app.get("/health", tags=["health"])
    def health() -> dict[str, Any]:
        """进程与任务健康，含最近失败数与最后成功时间。"""
        return _health_payload(active_settings, active_registry)

    @app.get("/health/ready", tags=["health"])
    def ready() -> dict[str, Any]:
        """就绪检查。

        本地/测试环境允许模型端点缺失（降级）；其他环境视为未就绪，
        避免演示时把降级悄悄当成正常。
        """
        payload = _health_payload(active_settings, active_registry)
        degraded: list[str] = []

        if not active_settings.model_configured:
            degraded.append("model")

        degradable = active_settings.app_env in DEGRADABLE_ENVS
        ready_flag = degradable or not degraded

        payload["ready"] = ready_flag
        payload["degraded"] = degraded
        if not ready_flag:
            payload["status"] = "degraded"
        return payload

    return app


app = create_app()