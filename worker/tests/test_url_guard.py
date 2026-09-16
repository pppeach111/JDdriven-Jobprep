"""URL 安全校验测试。

对应 shared_docs/13-risk-and-compliance.md 第 7 节"URL 请求防 SSRF"。
全部用例通过注入解析器完成，不依赖真实 DNS 与外网。
"""

from __future__ import annotations

import pytest

from careerpath_worker.crawler.url_guard import UnsafeUrlError, validate_url

PUBLIC_ADDRESS = "93.184.216.34"


def resolver(*addresses: str):
    """构造固定返回值的解析器。"""
    return lambda host: list(addresses)


class TestSchemeRestriction:
    """只允许 http/https。"""

    @pytest.mark.parametrize("url", ["http://example.com/a", "https://example.com/a"])
    def test_http_and_https_allowed(self, url: str) -> None:
        assert validate_url(url, resolver=resolver(PUBLIC_ADDRESS)) == url

    @pytest.mark.parametrize(
        "url",
        [
            "ftp://example.com/file",
            "file:///etc/passwd",
            "gopher://example.com/",
            "javascript:alert(1)",
            "data:text/html,<script>alert(1)</script>",
        ],
    )
    def test_non_http_schemes_rejected(self, url: str) -> None:
        with pytest.raises(UnsafeUrlError, match="仅允许 http/https"):
            validate_url(url, resolver=resolver(PUBLIC_ADDRESS))

    def test_scheme_check_happens_before_dns(self) -> None:
        """协议不合法时不应触发任何解析（避免无谓的外呼）。"""

        def exploding_resolver(host: str) -> list[str]:
            raise AssertionError("不应解析被拒绝的 URL")

        with pytest.raises(UnsafeUrlError):
            validate_url("file:///etc/passwd", resolver=exploding_resolver)


class TestEmptyAndMissingHost:
    @pytest.mark.parametrize("url", ["", "   ", "\t"])
    def test_blank_rejected(self, url: str) -> None:
        with pytest.raises(UnsafeUrlError, match="不能为空"):
            validate_url(url)

    def test_missing_host_rejected(self) -> None:
        with pytest.raises(UnsafeUrlError, match="缺少主机名"):
            validate_url("http:///path-only")


class TestBlockedAddresses:
    """内网、环回、链路本地与保留地址必须拒绝。"""

    @pytest.mark.parametrize(
        "address",
        [
            "127.0.0.1",
            "127.1.2.3",
            "10.0.0.5",
            "172.16.3.4",
            "192.168.1.1",
            "169.254.169.254",
            "100.64.0.1",
            "0.0.0.0",
            "224.0.0.1",
        ],
    )
    def test_blocked_ipv4_dns_result_rejected(self, address: str) -> None:
        with pytest.raises(UnsafeUrlError, match="被禁止"):
            validate_url("http://internal.example.com/", resolver=resolver(address))

    @pytest.mark.parametrize("address", ["::1", "fe80::1", "fc00::1", "::ffff:127.0.0.1"])
    def test_blocked_ipv6_dns_result_rejected(self, address: str) -> None:
        with pytest.raises(UnsafeUrlError, match="被禁止"):
            validate_url("http://internal.example.com/", resolver=resolver(address))

    def test_ip_literal_loopback_rejected(self) -> None:
        with pytest.raises(UnsafeUrlError, match="被禁止"):
            validate_url("http://127.0.0.1:8080/admin")

    def test_cloud_metadata_endpoint_rejected(self) -> None:
        """云元数据服务是 SSRF 的典型目标。"""
        with pytest.raises(UnsafeUrlError, match="被禁止"):
            validate_url("http://169.254.169.254/latest/meta-data/")

    def test_public_address_allowed(self) -> None:
        url = "https://careers.example.com/jobs/123"
        assert validate_url(url, resolver=resolver(PUBLIC_ADDRESS)) == url

    def test_mixed_public_and_private_rejected(self) -> None:
        """DNS 返回多条记录时，只要有一条内网就必须整体拒绝。"""
        with pytest.raises(UnsafeUrlError, match="被禁止"):
            validate_url(
                "http://mixed.example.com/",
                resolver=resolver(PUBLIC_ADDRESS, "10.0.0.7"),
            )

    def test_all_addresses_are_validated(self) -> None:
        with pytest.raises(UnsafeUrlError, match="被禁止"):
            validate_url(
                "http://mixed.example.com/",
                resolver=resolver(PUBLIC_ADDRESS, "169.254.169.254"),
            )


class TestOptOut:
    """本地联调允许显式放开，但不能是默认行为。"""

    def test_allow_private_permits_loopback(self) -> None:
        url = "http://127.0.0.1:8080/health"
        assert validate_url(url, allow_private=True) == url

    def test_allow_private_still_enforces_scheme(self) -> None:
        with pytest.raises(UnsafeUrlError, match="仅允许 http/https"):
            validate_url("file:///etc/passwd", allow_private=True)


class TestResolverFailures:
    def test_unresolvable_host_rejected(self) -> None:
        def failing_resolver(host: str) -> list[str]:
            raise OSError("name or service not known")

        with pytest.raises(UnsafeUrlError, match="域名解析失败"):
            validate_url("http://nope.invalid/", resolver=failing_resolver)

    def test_empty_resolution_rejected(self) -> None:
        with pytest.raises(UnsafeUrlError, match="无法解析主机"):
            validate_url("http://empty.example.com/", resolver=resolver())


class TestNormalisation:
    def test_surrounding_whitespace_trimmed(self) -> None:
        url = "https://careers.example.com/jobs/1"
        assert validate_url(f"  {url}  ", resolver=resolver(PUBLIC_ADDRESS)) == url

    def test_error_message_does_not_leak_original_secret_query(self) -> None:
        """错误信息不应回显完整敏感 URL。"""
        with pytest.raises(UnsafeUrlError) as excinfo:
            validate_url("ftp://user:secret-token@example.com/")
        assert "secret-token" not in str(excinfo.value)