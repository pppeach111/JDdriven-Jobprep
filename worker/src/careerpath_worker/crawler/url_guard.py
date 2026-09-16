"""URL 安全校验（防 SSRF）。

依据 shared_docs/13-risk-and-compliance.md 第 7 节：
    "URL 请求防 SSRF，只允许 HTTP/HTTPS 和受控网络策略。"

设计要点：
    - 只接受 http / https；
    - 拒绝环回、私网、链路本地、保留与多播地址；
    - 域名解析结果全部校验，避免 DNS 记录中混入内网地址；
    - 解析函数可注入，便于在无网络环境下测试。
"""

from __future__ import annotations

import ipaddress
import socket
from collections.abc import Callable, Iterable
from urllib.parse import urlsplit

__all__ = ["UnsafeUrlError", "validate_url", "resolve_host_addresses"]

ALLOWED_SCHEMES = frozenset({"http", "https"})

Resolver = Callable[[str], Iterable[str]]


class UnsafeUrlError(ValueError):
    """URL 未通过安全校验。"""


def resolve_host_addresses(host: str) -> list[str]:
    """默认解析器：返回域名对应的全部 IP 字符串。"""
    infos = socket.getaddrinfo(host, None, proto=socket.IPPROTO_TCP)
    addresses: list[str] = []
    for info in infos:
        sockaddr = info[4]
        if sockaddr and sockaddr[0] not in addresses:
            addresses.append(sockaddr[0])
    return addresses


#: Python 标准库未标记为 private、但在 SSRF 场景下同样必须禁止的网段。
#: 尤其是 100.64.0.0/10（RFC 6598 运营商级 NAT）不在标准库的 is_private 内，
#: 若不显式禁止会成为绕过点。
EXTRA_BLOCKED_NETWORKS = (
    ipaddress.ip_network("100.64.0.0/10"),
    ipaddress.ip_network("192.0.0.0/24"),
    ipaddress.ip_network("192.0.2.0/24"),
    ipaddress.ip_network("198.18.0.0/15"),
    ipaddress.ip_network("198.51.100.0/24"),
    ipaddress.ip_network("203.0.113.0/24"),
)


def _is_blocked(address: ipaddress.IPv4Address | ipaddress.IPv6Address) -> bool:
    """判断地址是否属于禁止访问的范围。"""
    if isinstance(address, ipaddress.IPv6Address) and address.ipv4_mapped is not None:
        # IPv4 映射地址（::ffff:127.0.0.1）需按其 IPv4 语义判断
        return _is_blocked(address.ipv4_mapped)

    if isinstance(address, ipaddress.IPv4Address) and any(
        address in network for network in EXTRA_BLOCKED_NETWORKS
    ):
        return True

    return any(
        (
            address.is_loopback,
            address.is_private,
            address.is_link_local,
            address.is_multicast,
            address.is_reserved,
            address.is_unspecified,
        )
    )


def validate_url(
    url: str,
    *,
    allow_private: bool = False,
    resolver: Resolver | None = None,
) -> str:
    """校验并返回规范化后的 URL。

    Args:
        url: 待校验的绝对 URL。
        allow_private: 是否允许访问内网地址（仅本地联调可开启）。
        resolver: 域名解析函数，默认使用系统解析。

    Raises:
        UnsafeUrlError: URL 为空、协议不允许、缺少主机，或解析出被禁止的地址。
    """
    if not url or not url.strip():
        raise UnsafeUrlError("URL 不能为空")

    candidate = url.strip()
    parts = urlsplit(candidate)

    scheme = parts.scheme.lower()
    if scheme not in ALLOWED_SCHEMES:
        raise UnsafeUrlError(f"仅允许 http/https，收到：{scheme or '(无协议)'}")

    host = parts.hostname
    if not host:
        raise UnsafeUrlError("URL 缺少主机名")

    if allow_private:
        return candidate

    addresses = _resolve(host, resolver)
    if not addresses:
        raise UnsafeUrlError(f"无法解析主机：{host}")

    for raw in addresses:
        try:
            address = ipaddress.ip_address(raw)
        except ValueError as exc:
            raise UnsafeUrlError(f"无法识别的地址：{raw}") from exc

        if _is_blocked(address):
            raise UnsafeUrlError(f"目标地址属于被禁止的内网或保留网段：{raw}")

    return candidate


def _resolve(host: str, resolver: Resolver | None) -> list[str]:
    """解析主机名；若 host 本身即为 IP 字面量则直接返回。"""
    try:
        return [str(ipaddress.ip_address(host))]
    except ValueError:
        pass

    active: Resolver = resolver or resolve_host_addresses
    try:
        return [str(item) for item in active(host)]
    except UnsafeUrlError:
        raise
    except OSError as exc:
        raise UnsafeUrlError(f"域名解析失败：{host}") from exc