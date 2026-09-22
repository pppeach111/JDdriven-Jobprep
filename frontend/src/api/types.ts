/**
 * 与后端契约一一对应的类型定义。
 *
 * 来源：shared_docs/14-contracts-and-schemas.md 与 backend 的 DTO 记录。
 * 约束（14-contracts-and-schemas.md）：
 *   - 字段名、枚举值不得自行改写；
 *   - 后端可能返回 null；null 表示"未知"，前端不得把它展示成"满足"。
 */

/** 统一成功响应外层，对应 ApiResponse<T>。 */
export interface ApiResponse<T> {
  schemaVersion: string
  data: T
  traceId: string
}

/** 统一失败响应外层，对应 ErrorResponse。 */
export interface ErrorBody {
  code: string
  message: string
  details: Record<string, unknown>
  retryable: boolean
}

export interface ErrorResponse {
  schemaVersion: string
  error: ErrorBody
  traceId: string
}

// ============================================================
// 求职目标
// ============================================================

export interface CreateGoalRequest {
  name: string
  jobFamily?: string | null
  city?: string | null
  employmentType?: string | null
  graduationYear?: number | null
  availableFrom?: string | null
  weeklyHours?: number | null
}

export type GoalStatus = 'ACTIVE' | 'ARCHIVED'

export interface JobGoal {
  id: string
  name: string
  jobFamily: string
  city: string | null
  employmentType: string | null
  graduationYear: number | null
  availableFrom: string | null
  weeklyHours: number | null
  status: GoalStatus
  createdAt: string
  updatedAt: string
}

// ============================================================
// JD 导入与解析
// ============================================================

export interface ImportJdRequest {
  rawText: string
  sourceUrl?: string | null
}

export type ParseStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'PARTIAL'
  | 'FAILED'
  | 'CANCELLED'

export interface JdImportResponse {
  sourceId: string
  parseStatus: ParseStatus
}

export type RequirementType = 'HARD_GATE' | 'CORE' | 'PREFERRED' | 'BONUS'

export interface RequirementView {
  id: string
  type: RequirementType
  field: string | null
  value: Record<string, unknown> | null
  skillId: string | null
  requiredLevel: number | null
  importance: number | null
  /** false 表示这是程序推断而非原文明确表述，前端必须显式区分。 */
  explicit: boolean
  sourceQuote: string | null
  sourceLocator: Record<string, unknown> | null
  confidence: number | null
}

export interface JobFamilyView {
  value: string
  confidence: number
}

export interface JdParseResult {
  schemaVersion: string
  sourceId: string
  jobFamily: JobFamilyView
  requirements: RequirementView[]
  /** 原文未提供、无法确定的字段。必须显示为"未知"，不得默认成满足。 */
  unknowns: string[]
  warnings: string[]
}

// ============================================================
// 能力图谱
// ============================================================

export type GapType =
  | 'QUALIFICATION_BLOCK'
  | 'KNOWLEDGE_GAP'
  | 'APPLICATION_GAP'
  | 'PRACTICE_GAP'
  | 'EVIDENCE_GAP'
  | 'EXPRESSION_GAP'
  | 'CONFLICT'

export interface NextAction {
  type: string
  /** 学习任务模块落地前为 null，表示尚未生成可执行任务，不得用占位 ID 冒充。 */
  taskId: string | null
  reason: string
}

export interface SkillCard {
  skillId: string
  name: string
  requiredLevel: number | null
  estimatedLevel: number | null
  /** 必须有值；契约要求界面同时展示等级与置信度，不得只给单一分数。 */
  confidence: number | null
  gapType: GapType | null
  dimensions: Record<string, number> | null
  evidenceIds: string[]
  sourceQuotes: string[]
  nextAction: NextAction | null
  updatedAt: string | null
}

export interface CapabilityMap {
  schemaVersion: string
  goalId: string
  skills: SkillCard[]
}
// ============================================================
// 能力证据（14 号契约第 11 节，2026-09-22 新增）
// ============================================================

export type EvidenceType =
  | 'SELF_CLAIM'
  | 'RESUME_CLAIM'
  | 'COURSE'
  | 'CERTIFICATE'
  | 'PROJECT'
  | 'CODE'
  | 'OBJECTIVE_TEST'
  | 'SCENARIO_TEST'
  | 'INTERVIEW'
  | 'MICRO_PRACTICE'
  | 'PROJECT_RESULT'

export type EvidenceDirection = 'SUPPORTS' | 'WEAKENS' | 'NEUTRAL'

export interface EvidenceLinkInput {
  skillId: string
  direction: EvidenceDirection
  /** SUPPORTS 时必填；WEAKENS/NEUTRAL 时可为 null。 */
  claimedLevel: number | null
  /** null 视为 1.0。 */
  strength: number | null
}

export interface RegisterEvidenceRequest {
  type: EvidenceType
  title: string
  contentSummary?: string | null
  /** null 时由后端类型基准权重决定，不得在前端伪造默认值。 */
  credibility?: number | null
  links: EvidenceLinkInput[]
}

export interface EvidenceLinkView {
  skillId: string
  skillName: string
  direction: EvidenceDirection
  strength: number | null
  claimedLevel: number | null
}

export interface UpdatedEstimate {
  skillId: string
  estimatedLevel: number | null
  confidence: number | null
  gapType: GapType | null
}

export interface EvidenceView {
  evidenceId: string
  type: EvidenceType
  title: string
  contentSummary: string | null
  credibility: number | null
  occurredAt: string | null
  createdAt: string
  links: EvidenceLinkView[]
  updatedEstimates: UpdatedEstimate[]
}