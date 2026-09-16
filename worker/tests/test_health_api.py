"""Worker 健康接口测试。

对应 shared_docs/17-environment-and-runbook.md 第 7 节：
    提供当前任务数、最近失败数和最后成功时间；
    本地可降级，演示环境必须明确显示降级状态。
"""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from careerpath_worker.api import create_app
from careerpath_worker.config import Settings
from careerpath_worker.tasks.models import ErrorCategory
from careerpath_worker.tasks.registry import TaskRegistry


def make_client(settings: Settings, registry: TaskRegistry | None = None) -> TestClient:
    return TestClient(create_app(settings, registry or TaskRegistry()))


class TestHealth:
    def test_health_reports_task_metrics(self) -> None:
        client = make_client(Settings(app_env="local"))
        payload = client.get("/health").json()

        assert payload["status"] == "ok"
        assert payload["tasks"]["running"] == 0
        assert payload["tasks"]["recentFailures"] == 0
        assert payload["tasks"]["lastSuccessAt"] is None
        assert "version" in payload

    def test_health_reflects_registry_state(self) -> None:
        registry = TaskRegistry()
        running, _ = registry.register("FETCH_PAGE", "k1")
        registry.mark_running(running.task_id)

        failed, _ = registry.register("FETCH_PAGE", "k2")
        registry.mark_running(failed.task_id)
        registry.mark_failed(failed.task_id, ErrorCategory.NETWORK, "boom")

        succeeded, _ = registry.register("FETCH_PAGE", "k3")
        registry.mark_running(succeeded.task_id)
        registry.mark_succeeded(succeeded.task_id)

        payload = make_client(Settings(app_env="local"), registry).get("/health").json()

        assert payload["tasks"]["running"] == 1
        assert payload["tasks"]["recentFailures"] == 1
        assert payload["tasks"]["lastSuccessAt"] is not None

    def test_health_lists_dependency_states(self) -> None:
        payload = make_client(Settings(app_env="local")).get("/health").json()
        assert payload["dependencies"] == {
            "database": "not_configured",
            "redis": "not_configured",
            "model": "not_configured",
        }

    def test_health_marks_configured_dependencies(self) -> None:
        settings = Settings(
            app_env="local",
            database_url="postgresql://localhost/db",
            redis_url="redis://localhost:6379/0",
            maas_base_url="https://maas.example.com",
            maas_model="demo-model",
            maas_api_key="secret",
        )
        payload = make_client(settings).get("/health").json()
        assert payload["dependencies"]["database"] == "configured"
        assert payload["dependencies"]["redis"] == "configured"
        assert payload["dependencies"]["model"] == "configured"

    def test_health_does_not_leak_secrets(self) -> None:
        settings = Settings(
            app_env="local",
            maas_base_url="https://maas.example.com",
            maas_model="demo-model",
            maas_api_key="super-secret-key",
        )
        body = make_client(settings).get("/health").text
        assert "super-secret-key" not in body


class TestReadiness:
    @pytest.mark.parametrize("env", ["local", "dev", "test"])
    def test_degradable_env_stays_ready_without_model(self, env: str) -> None:
        payload = make_client(Settings(app_env=env)).get("/health/ready").json()
        assert payload["ready"] is True
        assert payload["degraded"] == ["model"]

    @pytest.mark.parametrize("env", ["demo", "production"])
    def test_strict_env_not_ready_without_model(self, env: str) -> None:
        payload = make_client(Settings(app_env=env)).get("/health/ready").json()
        assert payload["ready"] is False
        assert payload["status"] == "degraded"
        assert payload["degraded"] == ["model"]

    def test_ready_when_model_configured(self) -> None:
        settings = Settings(
            app_env="production",
            maas_base_url="https://maas.example.com",
            maas_model="demo-model",
            maas_api_key="secret",
        )
        payload = make_client(settings).get("/health/ready").json()
        assert payload["ready"] is True
        assert payload["degraded"] == []
        assert payload["status"] == "ok"