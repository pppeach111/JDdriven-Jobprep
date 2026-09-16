"""Worker 配置。

来源：shared_docs/17-environment-and-runbook.md 第 3、4、8 节。

约定：
    - 所有外部依赖地址与密钥只能来自环境变量，不写死在代码中；
    - 缺失的可选值保持 None，不用空字符串冒充"已配置"；
    - 超时上限在配置中固定，便于测试与审计。
"""

from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """通过环境变量注入的运行配置。"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # ---------- 运行环境 ----------
    app_env: str = "local"
    worker_port: int = 8000
    worker_host: str = "127.0.0.1"

    # ---------- 依赖服务（本地可缺省，演示环境必须配置）----------
    database_url: str | None = None
    redis_url: str | None = None
    maas_base_url: str | None = None
    maas_model: str | None = None
    maas_api_key: str | None = None
    obs_endpoint: str | None = None
    obs_bucket: str | None = None

    # ---------- 采集约束（13-risk-and-compliance.md 第 6、7 节）----------
    crawler_user_agent: str = "CareerPathResearchBot/0.1 (+contact)"
    crawler_max_concurrency: int = 2
    crawler_min_interval_ms: int = 1500

    # ---------- 超时上限（17-environment-and-runbook.md 第 8 节）----------
    fetch_timeout_seconds: float = 30.0
    model_timeout_seconds: float = 60.0
    task_timeout_seconds: float = 300.0

    # ---------- 重试策略 ----------
    task_max_attempts: int = 3
    task_backoff_base_seconds: float = 1.0
    task_backoff_max_seconds: float = 30.0

    # ---------- 安全 ----------
    # 本地联调可能需要访问内网地址；演示与生产必须保持 False。
    allow_private_network_urls: bool = False

    @property
    def model_configured(self) -> bool:
        """模型端点是否已完整配置。未配置时相关能力应降级而非静默失败。"""
        return bool(self.maas_base_url and self.maas_model and self.maas_api_key)


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """进程内复用同一份配置。"""
    return Settings()