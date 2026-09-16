"""任务注册表与健康指标。

内存实现用于首版与本地开发；接入 Redis/数据库后可替换为持久化实现，
调用方只依赖本模块暴露的方法签名。

健康指标依据 shared_docs/17-environment-and-runbook.md 第 7 节：
    "Worker 至少提供当前任务数、最近失败数和最后成功时间；
     不能以进程存活代替任务健康。"
"""

from __future__ import annotations

import threading
import uuid
from dataclasses import dataclass
from datetime import datetime

from careerpath_worker.tasks.models import (
    ErrorCategory,
    TaskRecord,
    TaskStatus,
    utc_now,
)


def backoff_delay(
    attempt: int,
    *,
    base_seconds: float = 1.0,
    max_seconds: float = 30.0,
) -> float:
    """指数退避延迟。

    Args:
        attempt: 第几次尝试，从 1 开始计数。
        base_seconds: 首次退避基数。
        max_seconds: 退避上限，避免无限增长。

    Returns:
        建议等待的秒数；attempt <= 1 时返回 0（首次失败不等待）。
    """
    if attempt <= 1:
        return 0.0
    delay = base_seconds * (2 ** (attempt - 2))
    return float(min(delay, max_seconds))


@dataclass(frozen=True)
class HealthSnapshot:
    """供 /health 暴露的任务健康快照。"""

    running_tasks: int
    recent_failures: int
    last_success_at: datetime | None


class TaskRegistry:
    """线程安全的内存任务注册表。"""

    def __init__(self, *, recent_failure_window: int = 50) -> None:
        self._lock = threading.RLock()
        self._tasks: dict[str, TaskRecord] = {}
        self._by_idempotency_key: dict[str, str] = {}
        self._recent_failures: list[datetime] = []
        self._recent_failure_window = recent_failure_window
        self._last_success_at: datetime | None = None

    # ---------------------------------------------------------------- 幂等
    def register(
        self,
        task_type: str,
        idempotency_key: str,
        *,
        max_attempts: int = 3,
    ) -> tuple[TaskRecord, bool]:
        """注册任务。

        相同 idempotency_key 重复调用时返回已有记录，避免重复执行与重复写入。

        Returns:
            (任务记录, 是否新建)。
        """
        if not idempotency_key or not idempotency_key.strip():
            raise ValueError("idempotency_key 不能为空")

        with self._lock:
            existing_id = self._by_idempotency_key.get(idempotency_key)
            if existing_id is not None:
                return self._tasks[existing_id], False

            record = TaskRecord(
                task_id=str(uuid.uuid4()),
                task_type=task_type,
                idempotency_key=idempotency_key,
                max_attempts=max_attempts,
            )
            self._tasks[record.task_id] = record
            self._by_idempotency_key[idempotency_key] = record.task_id
            return record, True

    # ---------------------------------------------------------------- 生命周期
    def mark_running(self, task_id: str) -> TaskRecord:
        with self._lock:
            record = self._require(task_id)
            record.status = TaskStatus.RUNNING
            record.attempts += 1
            record.started_at = record.started_at or utc_now()
            record.touch()
            return record

    def mark_succeeded(self, task_id: str, *, partial: bool = False) -> TaskRecord:
        with self._lock:
            record = self._require(task_id)
            record.status = TaskStatus.PARTIAL if partial else TaskStatus.SUCCEEDED
            record.progress = 1.0
            record.finished_at = utc_now()
            record.touch()
            if not partial:
                self._last_success_at = record.finished_at
            return record

    def mark_failed(self, task_id: str, category: ErrorCategory, message: str) -> TaskRecord:
        with self._lock:
            record = self._require(task_id)
            record.status = TaskStatus.FAILED
            record.last_error_category = category
            record.last_error_message = message
            record.finished_at = utc_now()
            record.touch()
            self._recent_failures.append(record.finished_at)
            self._recent_failures = self._recent_failures[-self._recent_failure_window :]
            return record

    def mark_cancelled(self, task_id: str) -> TaskRecord:
        with self._lock:
            record = self._require(task_id)
            record.status = TaskStatus.CANCELLED
            record.finished_at = utc_now()
            record.touch()
            return record

    # ---------------------------------------------------------------- 查询
    def get(self, task_id: str) -> TaskRecord | None:
        with self._lock:
            return self._tasks.get(task_id)

    def find_by_idempotency_key(self, idempotency_key: str) -> TaskRecord | None:
        with self._lock:
            task_id = self._by_idempotency_key.get(idempotency_key)
            return self._tasks.get(task_id) if task_id else None

    def snapshot(self) -> HealthSnapshot:
        """当前任务健康指标。"""
        with self._lock:
            running = sum(
                1 for record in self._tasks.values() if record.status is TaskStatus.RUNNING
            )
            return HealthSnapshot(
                running_tasks=running,
                recent_failures=len(self._recent_failures),
                last_success_at=self._last_success_at,
            )

    def _require(self, task_id: str) -> TaskRecord:
        record = self._tasks.get(task_id)
        if record is None:
            raise KeyError(f"任务不存在：{task_id}")
        return record


#: 进程级默认注册表。测试应自行构造独立实例，避免相互影响。
default_registry = TaskRegistry()