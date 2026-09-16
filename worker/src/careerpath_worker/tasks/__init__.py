"""异步任务模型与调度。

依据 shared_docs/17-environment-and-runbook.md 第 8 节，所有抓取、解析、模型和
计划任务必须：
    - 使用幂等键；
    - 持久化状态；
    - 限制重试次数；
    - 指数退避；
    - 记录最后错误类别；
    - 支持取消或超时；
    - 不重复写入 Evidence。
"""

from careerpath_worker.tasks.models import (
    ErrorCategory,
    TaskRecord,
    TaskStatus,
)
from careerpath_worker.tasks.registry import TaskRegistry, backoff_delay

__all__ = [
    "ErrorCategory",
    "TaskRecord",
    "TaskStatus",
    "TaskRegistry",
    "backoff_delay",
]