"""职径（CareerPath）Python Worker。

职责（02-system-architecture.md 第 2 节）：
    - 官方岗位发现与页面解析；
    - JD / 简历结构化提取；
    - 测评开放题辅助评分；
    - 动态面试追问；
    - 学习资源候选生成与核验。

边界（02-system-architecture.md 2.1、17-environment-and-runbook.md 第 9 节）：
    Spring Boot 是唯一主业务 API；本 Worker 通过异步任务或内部接口提供能力，
    不对外承担主 API 契约。
"""

__version__ = "0.1.0"