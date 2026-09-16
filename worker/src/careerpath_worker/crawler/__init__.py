"""采集与页面解析能力。

依据 shared_docs/02-system-architecture.md 2.1：
    首版采用"静态解析库 + 配置选择器"，不平台化引入 Scrapy；
    Playwright 仅作为静态抓取缺正文时的二级方案，且不绕过登录、验证码或反爬。
"""

from careerpath_worker.crawler.url_guard import UnsafeUrlError, validate_url

__all__ = ["UnsafeUrlError", "validate_url"]