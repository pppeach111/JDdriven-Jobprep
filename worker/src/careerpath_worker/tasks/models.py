"""任务数据模型。

状态枚举与 shared_docs/14-contracts-and-schemas.md 第 2.1 节 TaskStatus 保持一致，
不得自行改写取值。
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum


def utc_now() -> datetime:
    """统一使用带时区的 UTC 时间，对应契约中的 ISO-8601。"""
    return datetime.now(timezone.utc)


class TaskStatus(str, Enum):
    """对齐 14-contracts-and-schemas.md 第 2.1 节 TaskStatus。"""

    PENDING = "PENDING"
    RUNNING = "RUNNING"
    SUCCEEDED = "SUCCEEDED"
    PARTIAL = "PARTIAL"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class ErrorCategory(str, Enum):
    """最后错误类别，用于区分可重试与不可重试失败。"""

    NETWORK = "NETWORK"
    TIMEOUT = "TIMEOUT"
    PARSE = "PARSE"
    MODEL = "MODEL"
    VALIDATION = "VALIDATION"
    UNKNOWN = "UNKNOWN"


#: 只有这些类别允许继续重试；其余视为确定性失败，重试无意义。
RETRYABLE_CATEGORIES = frozenset(
    {ErrorCategory.NETWORK, ErrorCategory.TIMEOUT, ErrorCategory.MODEL}
)

TERMINAL_STATUSES = frozenset(
    {
        TaskStatus.SUCCEEDED,
        TaskStatus.PARTIAL,
        TaskStatus.FAILED,
        TaskStatus.CANCELLED,
    }
)


@dataclass
class TaskRecord:
    """单个异步任务的状态记录。"""

    task_id: str
    task_type: str
    idempotency_key: str
    status: TaskStatus = TaskStatus.PENDING
    attempts: int = 0
    max_attempts: int = 3
    last_error_category: ErrorCategory | None = None
    last_error_message: str | None = None
    progress: float = 0.0
    created_at: datetime = field(default_factory=utc_now)
    updated_at: datetime = field(default_factory=utc_now)
    started_at: datetime | None = None
    finished_at: datetime | None = None

    @property
    def is_terminal(self) -> bool:
        return self.status in TERMINAL_STATUSES

    @property
    def can_retry(self) -> bool:
        """达到次数上限或错误类别不可重试时，不再重试。"""
        if self.attempts >= self.max_attempts:
            return False
        if self.last_error_category is None:
            return self.attempts < self.max_attempts
        return self.last_error_category in RETRYABLE_CATEGORIES

    def touch(self) -> None:
        self.updated_at = utc_now()