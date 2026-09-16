"""任务注册表、幂等与重试策略测试。

对应 shared_docs/17-environment-and-runbook.md 第 8 节：
    幂等键、限制重试次数、指数退避、记录最后错误类别、支持取消或超时。
"""

from __future__ import annotations

import pytest

from careerpath_worker.tasks.models import (
    ErrorCategory,
    TaskStatus,
    utc_now,
)
from careerpath_worker.tasks.registry import TaskRegistry, backoff_delay


class TestBackoff:
    def test_first_attempt_has_no_delay(self) -> None:
        assert backoff_delay(1) == 0.0

    def test_delay_grows_exponentially(self) -> None:
        assert backoff_delay(2, base_seconds=1.0) == 1.0
        assert backoff_delay(3, base_seconds=1.0) == 2.0
        assert backoff_delay(4, base_seconds=1.0) == 4.0

    def test_delay_is_capped(self) -> None:
        assert backoff_delay(20, base_seconds=1.0, max_seconds=30.0) == 30.0

    def test_zero_attempt_has_no_delay(self) -> None:
        assert backoff_delay(0) == 0.0


class TestIdempotency:
    def test_same_key_returns_same_record(self) -> None:
        registry = TaskRegistry()
        first, created_first = registry.register("JD_EXTRACT", "goal-1:jd-1")
        second, created_second = registry.register("JD_EXTRACT", "goal-1:jd-1")

        assert created_first is True
        assert created_second is False
        assert first.task_id == second.task_id

    def test_different_keys_create_distinct_tasks(self) -> None:
        registry = TaskRegistry()
        first, _ = registry.register("JD_EXTRACT", "key-a")
        second, _ = registry.register("JD_EXTRACT", "key-b")
        assert first.task_id != second.task_id

    @pytest.mark.parametrize("key", ["", "   "])
    def test_blank_key_rejected(self, key: str) -> None:
        registry = TaskRegistry()
        with pytest.raises(ValueError, match="idempotency_key 不能为空"):
            registry.register("JD_EXTRACT", key)

    def test_lookup_by_idempotency_key(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("JD_EXTRACT", "key-a")
        found = registry.find_by_idempotency_key("key-a")
        assert found is not None
        assert found.task_id == record.task_id

    def test_lookup_missing_key_returns_none(self) -> None:
        registry = TaskRegistry()
        assert registry.find_by_idempotency_key("absent") is None


class TestLifecycle:
    def test_pending_by_default(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("JD_EXTRACT", "key")
        assert record.status is TaskStatus.PENDING
        assert record.attempts == 0
        assert record.is_terminal is False

    def test_running_increments_attempts(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("JD_EXTRACT", "key")

        registry.mark_running(record.task_id)
        assert record.status is TaskStatus.RUNNING
        assert record.attempts == 1
        assert record.started_at is not None

        registry.mark_running(record.task_id)
        assert record.attempts == 2

    def test_succeeded_sets_progress_and_terminal(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("JD_EXTRACT", "key")
        registry.mark_running(record.task_id)
        registry.mark_succeeded(record.task_id)

        assert record.status is TaskStatus.SUCCEEDED
        assert record.progress == 1.0
        assert record.finished_at is not None
        assert record.is_terminal is True

    def test_partial_is_terminal_but_not_success(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("JD_EXTRACT", "key")
        registry.mark_running(record.task_id)
        registry.mark_succeeded(record.task_id, partial=True)

        assert record.status is TaskStatus.PARTIAL
        assert record.is_terminal is True
        assert registry.snapshot().last_success_at is None

    def test_cancelled(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("JD_EXTRACT", "key")
        registry.mark_cancelled(record.task_id)
        assert record.status is TaskStatus.CANCELLED
        assert record.is_terminal is True

    def test_unknown_task_raises(self) -> None:
        registry = TaskRegistry()
        with pytest.raises(KeyError):
            registry.mark_running("missing-task-id")


class TestFailureAndRetry:
    def test_failure_records_category_and_message(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("FETCH_PAGE", "key")
        registry.mark_running(record.task_id)
        registry.mark_failed(record.task_id, ErrorCategory.TIMEOUT, "超过 30 秒")

        assert record.status is TaskStatus.FAILED
        assert record.last_error_category is ErrorCategory.TIMEOUT
        assert record.last_error_message == "超过 30 秒"
        assert record.finished_at is not None

    @pytest.mark.parametrize(
        "category",
        [ErrorCategory.NETWORK, ErrorCategory.TIMEOUT, ErrorCategory.MODEL],
    )
    def test_retryable_categories_stay_retryable(self, category: ErrorCategory) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("FETCH_PAGE", "key", max_attempts=3)
        registry.mark_running(record.task_id)
        registry.mark_failed(record.task_id, category, "transient")
        assert record.can_retry is True

    @pytest.mark.parametrize(
        "category",
        [ErrorCategory.VALIDATION, ErrorCategory.PARSE, ErrorCategory.UNKNOWN],
    )
    def test_non_retryable_categories_stop(self, category: ErrorCategory) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("FETCH_PAGE", "key", max_attempts=3)
        registry.mark_running(record.task_id)
        registry.mark_failed(record.task_id, category, "deterministic")
        assert record.can_retry is False

    def test_retry_stops_at_max_attempts(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("FETCH_PAGE", "key", max_attempts=2)

        registry.mark_running(record.task_id)
        registry.mark_failed(record.task_id, ErrorCategory.NETWORK, "boom")
        assert record.can_retry is True

        registry.mark_running(record.task_id)
        registry.mark_failed(record.task_id, ErrorCategory.NETWORK, "boom")
        assert record.attempts == 2
        assert record.can_retry is False


class TestHealthSnapshot:
    def test_empty_registry(self) -> None:
        snapshot = TaskRegistry().snapshot()
        assert snapshot.running_tasks == 0
        assert snapshot.recent_failures == 0
        assert snapshot.last_success_at is None

    def test_counts_only_running(self) -> None:
        registry = TaskRegistry()
        running, _ = registry.register("A", "k1")
        done, _ = registry.register("B", "k2")

        registry.mark_running(running.task_id)
        registry.mark_running(done.task_id)
        registry.mark_succeeded(done.task_id)

        snapshot = registry.snapshot()
        assert snapshot.running_tasks == 1
        assert snapshot.last_success_at is not None

    def test_recent_failures_accumulate(self) -> None:
        registry = TaskRegistry()
        for index in range(3):
            record, _ = registry.register("A", f"k{index}")
            registry.mark_running(record.task_id)
            registry.mark_failed(record.task_id, ErrorCategory.NETWORK, "boom")
        assert registry.snapshot().recent_failures == 3

    def test_recent_failures_window_is_bounded(self) -> None:
        registry = TaskRegistry(recent_failure_window=2)
        for index in range(5):
            record, _ = registry.register("A", f"k{index}")
            registry.mark_running(record.task_id)
            registry.mark_failed(record.task_id, ErrorCategory.NETWORK, "boom")
        assert registry.snapshot().recent_failures == 2

    def test_last_success_tracks_latest(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("A", "k1")
        registry.mark_running(record.task_id)
        registry.mark_succeeded(record.task_id)
        first_success = registry.snapshot().last_success_at
        assert first_success is not None

        later, _ = registry.register("A", "k2")
        registry.mark_running(later.task_id)
        registry.mark_succeeded(later.task_id)
        second_success = registry.snapshot().last_success_at

        assert second_success is not None
        assert second_success >= first_success


class TestRecordHelpers:
    def test_touch_updates_timestamp(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("A", "k1")
        before = record.updated_at
        record.touch()
        assert record.updated_at >= before

    def test_can_retry_before_any_failure(self) -> None:
        registry = TaskRegistry()
        record, _ = registry.register("A", "k1", max_attempts=3)
        assert record.can_retry is True

    def test_utc_now_is_timezone_aware(self) -> None:
        assert utc_now().tzinfo is not None