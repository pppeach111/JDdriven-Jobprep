/**
 * 明确标识的演示数据 —— 岗位发现 / 测评与面试 / 学习计划 / 简历建议
 *
 * 为什么存在：这 4 个页面的后端接口尚未实现（见 shared_docs/18-current-state-and-next-actions.md），
 * 而 19-ui-design-system.md §15 要求先完成界面与状态设计。因此这里提供**固定演示数据**，
 * 让界面可以被真实评审，且不伪造任何接口调用。
 *
 * 约束（不满足即视为缺陷）：
 *   - 使用方必须在标题区用 `BeautifulStatus label="演示数据" tone="warn"` 明确标注来源，
 *     符合 §14「不得用 Mock 冒充真实集成」的例外条件；
 *   - 这里的数据类型**不是已冻结契约**。shared_docs/14-contracts-and-schemas.md 一旦定义这些
 *     资源，必须改为从 `@/api/types` 引入，并换成真实请求；
 *   - 所有"未知"一律写 `null` / `UNKNOWN`，不得为了让界面好看而填成已知；
 *   - 不写无法核算的百分比：`matchCoverage` 这类比率必须同时给出分子与分母。
 */

/** 页面顶部目标摘要。字段名沿用 14 号文档已冻结的 JobGoal，避免自造字段名。 */
export interface DemoGoalSummary {
  id: string
  name: string
  jobFamily: string
  city: string | null
  employmentType: string | null
  graduationYear: number | null
  weeklyHours: number | null
  /** 数据新鲜度：本批演示数据的基准时间。 */
  dataUpdatedAt: string
}

export const DEMO_GOAL: DemoGoalSummary = {
  id: 'demo-goal-java-backend',
  name: 'Java 后端开发实习生（2026 届）',
  jobFamily: 'JAVA_BACKEND',
  city: '深圳',
  employmentType: 'INTERNSHIP',
  graduationYear: 2026,
  weeklyHours: 20,
  dataUpdatedAt: '2026-09-21T09:30:00+08:00',
}

// ============================================================
// 岗位发现（19 号文档 §6.3 / §7.4）
// ============================================================

/**
 * 「现在适合投 / 补齐后再投 / 冲刺 / 硬门槛不符 / 状态待确认」是**分组**，不是卡片皮肤（§6.3）。
 */
export type DemoJobAvailability = 'READY' | 'AFTER_GAP' | 'STRETCH' | 'BLOCKED' | 'UNVERIFIED'

export type DemoJobOpenStatus = 'OPEN' | 'CLOSED' | 'UNKNOWN'

/** 匹配构成的分项：§6.3 要求匹配度旁必须可查看构成，不能只给百分比。 */
export interface DemoMatchFactor {
  label: string
  /** 该项占权重的分子。 */
  matched: number
  /** 该项占权重的分母。 */
  total: number
  note: string
}

export interface DemoHardGate {
  label: string
  /** true 满足 / false 不满足 / null 未知（原文未明确或尚未核验）。 */
  met: boolean | null
  note: string
}

export interface DemoJobGap {
  skillId: string
  name: string
  requiredLevel: number | null
  estimatedLevel: number | null
  confidence: number | null
}

export interface DemoJob {
  id: string
  title: string
  company: string
  locations: string[]
  employmentType: string
  openStatus: DemoJobOpenStatus
  availability: DemoJobAvailability
  matchFactors: DemoMatchFactor[]
  hardGates: DemoHardGate[]
  gaps: DemoJobGap[]
  /**
   * 来源必须可见且不折叠（§7.4）。
   *
   * `url` 一律为 null：§6.3 要求「申请入口只有在来源官方且 URL 已通过校验时出现」，
   * 演示数据里的链接无法校验，因此不提供任何跳转入口，只展示来源名称。
   */
  source: { name: string; url: string | null }
  /** 最后核验时间。null 表示从未核验，界面须提示重新核验。 */
  lastVerifiedAt: string | null
}

export const DEMO_JOBS: DemoJob[] = [
  {
    id: 'job-001',
    title: 'Java 后端开发实习生',
    company: '华创云科技',
    locations: ['深圳'],
    employmentType: 'INTERNSHIP · 实习',
    openStatus: 'OPEN',
    availability: 'READY',
    matchFactors: [
      { label: '核心技能', matched: 4, total: 5, note: 'Java、Spring Boot、MyBatis 已达目标等级' },
      { label: '数据库', matched: 1, total: 2, note: 'MySQL 索引优化 L3，Redis 缓存设计仍为 L2' },
      { label: '工程实践', matched: 1, total: 2, note: '缺少并发场景的实践产物' },
    ],
    hardGates: [
      { label: '毕业年份 2027 届', met: false, note: '你为 2026 届，该岗位标注 2027 届' },
      { label: '本科及以上', met: true, note: '原文明确表述' },
      { label: '实习时长 ≥ 3 个月', met: null, note: '原文未说明可协商范围' },
    ],
    gaps: [
      { skillId: 'redis-cache-design', name: 'Redis 缓存设计', requiredLevel: 3, estimatedLevel: 2, confidence: 0.78 },
      { skillId: 'java-concurrency', name: 'Java 并发编程', requiredLevel: 3, estimatedLevel: null, confidence: null },
    ],
    source: { name: '企业招聘官网', url: null },
    lastVerifiedAt: '2026-09-20T18:10:00+08:00',
  },
  {
    id: 'job-002',
    title: '后端开发工程师（校招提前批）',
    company: '远航数据技术有限公司',
    locations: ['杭州', '上海'],
    employmentType: 'CAMPUS · 校招',
    openStatus: 'OPEN',
    availability: 'AFTER_GAP',
    matchFactors: [
      { label: '核心技能', matched: 3, total: 5, note: 'Spring Boot 已达标，并发与分布式为知识差距' },
      { label: '数据库', matched: 1, total: 2, note: 'Redis 缓存设计需补到 L3' },
      { label: '项目经历', matched: 1, total: 1, note: '课程项目已覆盖基础 CRUD 场景' },
    ],
    hardGates: [
      { label: '本科及以上', met: true, note: '原文明确表述' },
      { label: '毕业年份 2026 届', met: true, note: '原文明确表述' },
    ],
    gaps: [
      { skillId: 'redis-cache-design', name: 'Redis 缓存设计', requiredLevel: 3, estimatedLevel: 2, confidence: 0.78 },
      { skillId: 'distributed-basics', name: '分布式基础', requiredLevel: 2, estimatedLevel: 1, confidence: 0.64 },
    ],
    source: { name: '校园招聘平台', url: null },
    lastVerifiedAt: '2026-09-18T10:05:00+08:00',
  },
  {
    id: 'job-003',
    title: '后端开发实习生（Java 方向）',
    company: '启岸信息技术（深圳）有限公司',
    locations: ['深圳', '广州', '东莞'],
    employmentType: 'INTERNSHIP · 实习',
    openStatus: 'OPEN',
    availability: 'STRETCH',
    matchFactors: [
      { label: '核心技能', matched: 3, total: 6, note: '要求 L4 的分布式与性能调优明显高于当前估计' },
      { label: '数据库', matched: 2, total: 3, note: 'MySQL 达标，Redis 与分库分表需补' },
    ],
    hardGates: [
      { label: '本科及以上', met: true, note: '原文明确表述' },
      { label: '每周到岗 ≥ 4 天', met: null, note: '你登记每周可投入 20 小时，原文未说明是否可折算' },
    ],
    gaps: [
      { skillId: 'system-performance', name: '性能调优', requiredLevel: 4, estimatedLevel: 2, confidence: 0.71 },
      { skillId: 'sharding', name: '分库分表', requiredLevel: 3, estimatedLevel: null, confidence: null },
    ],
    source: { name: '企业招聘官网', url: null },
    lastVerifiedAt: '2026-09-19T15:42:00+08:00',
  },
  {
    id: 'job-004',
    title: 'Java 开发工程师',
    company: '恒通软件集团有限公司',
    locations: ['深圳'],
    employmentType: 'CAMPUS · 校招',
    openStatus: 'CLOSED',
    availability: 'BLOCKED',
    matchFactors: [
      { label: '核心技能', matched: 4, total: 5, note: '技能基本匹配，但学历门槛未满足' },
    ],
    hardGates: [
      { label: '硕士研究生及以上', met: false, note: '原文明确表述，当前学历不满足' },
      { label: '毕业年份 2026 届', met: true, note: '原文明确表述' },
    ],
    gaps: [],
    source: { name: '企业招聘官网', url: null },
    lastVerifiedAt: '2026-09-12T09:00:00+08:00',
  },
  {
    id: 'job-005',
    title: '后端开发实习生',
    company: '南岭网络科技有限公司',
    locations: ['深圳', '长沙'],
    employmentType: 'INTERNSHIP · 实习',
    openStatus: 'UNKNOWN',
    availability: 'UNVERIFIED',
    matchFactors: [
      { label: '核心技能', matched: 3, total: 4, note: '已按原文完成比对，但岗位状态未核验' },
      { label: '数据库', matched: 1, total: 2, note: 'Redis 缓存设计需补到 L3' },
    ],
    hardGates: [
      { label: '本科及以上', met: true, note: '原文明确表述' },
      { label: '到岗时间', met: null, note: '原文未说明' },
    ],
    gaps: [
      { skillId: 'redis-cache-design', name: 'Redis 缓存设计', requiredLevel: 3, estimatedLevel: 2, confidence: 0.78 },
    ],
    source: { name: '招聘信息聚合页', url: null },
    lastVerifiedAt: null,
  },
]

export const JOB_AVAILABILITY_LABELS: Record<DemoJobAvailability, string> = {
  READY: '现在适合投',
  AFTER_GAP: '补齐后再投',
  STRETCH: '冲刺',
  BLOCKED: '硬门槛不符',
  UNVERIFIED: '状态待确认',
}

export const JOB_OPEN_STATUS_LABELS: Record<DemoJobOpenStatus, string> = {
  OPEN: '开放中',
  CLOSED: '已关闭',
  UNKNOWN: '开放状态未知',
}

// ============================================================
// 测评与面试（19 号文档 §7.5）
// ============================================================

/** §6.4：异步任务显示阶段，不显示假百分比。 */
export const DEMO_ASSESSMENT_STAGES = [
  { key: 'PREPARE', label: '准备题目', state: 'DONE' as const },
  { key: 'ANSWER', label: '作答', state: 'RUNNING' as const },
  { key: 'SUBMIT', label: '提交', state: 'PENDING' as const },
  { key: 'SCORE', label: '评分与生成报告', state: 'PENDING' as const },
]

export interface DemoAssessmentItem {
  id: string
  /** 题号（从 1 开始）。 */
  index: number
  kind: 'QUESTION' | 'FOLLOW_UP'
  skillId: string | null
  skillName: string
  prompt: string
  /** 面试追问必须说明"为什么问"，但不泄露评分答案（§7.5）。 */
  whyAsked: string | null
  /** 上一题的回答摘要，用于展示评分报告前的证据。 */
  previousAnswer: string | null
}

export const DEMO_ASSESSMENT = {
  id: 'demo-assessment-001',
  title: 'Java 后端能力测评',
  total: 8,
  currentIndex: 3,
  /** 保存状态：用户离开后可恢复（§7.5）。 */
  savedAt: '2026-09-21T09:12:04+08:00',
  resumedNote: '上次进度已保留：第 3 题（未提交）。离开页面不会丢作答。',
  items: [
    {
      id: 'q-01',
      index: 1,
      kind: 'QUESTION',
      skillId: 'java-collections',
      skillName: 'Java 集合框架',
      prompt: '请说明 HashMap 在 JDK 8 中的扩容机制，并解释为什么负载因子默认是 0.75。',
      whyAsked: null,
      previousAnswer:
        '答：JDK 8 在链表长度超过 8 且数组长度达到 64 时转为红黑树；扩容时按容量翻倍并在高位重新分布节点……',
    },
    {
      id: 'q-02',
      index: 2,
      kind: 'QUESTION',
      skillId: 'mysql-index',
      skillName: 'MySQL 索引优化',
      prompt: '有一条查询在 user_id 与 created_at 上分别建了单列索引，执行计划仍走全表扫描，你会怎么排查？',
      whyAsked: null,
      previousAnswer: '答：先看 EXPLAIN 的 key 与 rows，确认是否因函数包裹或隐式类型转换导致索引失效……',
    },
    {
      id: 'q-03',
      index: 3,
      kind: 'QUESTION',
      skillId: 'redis-cache-design',
      skillName: 'Redis 缓存设计',
      prompt:
        '一个热点商品详情接口在高峰期出现缓存击穿。请给出你的缓存方案，并说明缓存粒度、过期策略与失效时的降级方式。',
      whyAsked: null,
      previousAnswer: null,
    },
  ] as DemoAssessmentItem[],
  /** 面试追问示例：说明"为什么问"，不泄露评分答案。 */
  followUp: {
    id: 'q-04',
    index: 4,
    kind: 'FOLLOW_UP' as const,
    skillId: 'redis-cache-design',
    skillName: 'Redis 缓存设计',
    prompt: '你提到用互斥锁兜住数据库压力。如果这把锁本身超时了，请求会怎样？',
    whyAsked:
      '你上一题的回答只覆盖了缓存的使用方式，没有说明失效路径下的失败处理。这一问用于确认你是否真正处理过缓存不可用时的降级，而不是只停留在使用层面。',
    previousAnswer: null,
  },
}

/** 评分报告（§7.5：先展示能力变化与证据，再展示长篇模型总结）。 */
export const DEMO_ASSESSMENT_REPORT = {
  available: false,
  /** 若可用，展示为 "L2 → L3" 与变化原因，而不只是上升箭头（§6.1）。 */
  levelChanges: [
    {
      skillId: 'java-collections',
      name: 'Java 集合框架',
      from: 2,
      to: 3,
      reason: '能说明扩容阈值与红黑树转换条件，并解释负载因子的取舍',
      evidenceCount: 1,
      confidence: 0.72,
    },
  ],
  /** 报告未生成时，必须说明原因而不是显示空图表。 */
  unavailableReason: '本次测评尚未提交，评分报告不可用。提交后此处展示能力变化、证据和完整报告。',
  summary: null as string | null,
}

// ============================================================
// 学习计划（19 号文档 §7.6）
// ============================================================

export interface DemoLearningTask {
  id: string
  title: string
  /** 对应差距：让用户知道任务不是凭空安排的。 */
  gapLabel: string
  estimatedMinutes: number
  artifact: string
  acceptance: string[]
  retest: string
  /** 前置任务 id；未满足时界面必须说明原因（§7.6）。 */
  dependsOn: string | null
  done: boolean
}

export const DEMO_LEARNING_WEEK = {
  weekLabel: '2026-09-21 ～ 2026-09-27',
  /** 时间预算：用户登记的每周可投入小时数。 */
  budgetHours: 20,
  plannedMinutes: 540,
  adjustedNote: '本周计划由 8 小时调整为 9 小时：Redis 缓存设计差距优先级上调，因此把「并发编程」后移一周。',
  tasks: [
    {
      id: 'task-001',
      title: 'Redis 缓存设计：击穿、穿透、雪崩的三种失效路径',
      gapLabel: '知识差距 · Redis 缓存设计 L2 → L3',
      estimatedMinutes: 180,
      artifact: '一份含三种失效路径的笔记，以及每类对应的降级方案对比表',
      acceptance: [
        '能画出缓存读取与写入两条路径，并标注失效点',
        '对每类失效给出至少一种可实施方案，并说明代价',
        '方案中明确指出缓存不可用时接口的返回值',
      ],
      retest: '完成后重做「Redis 缓存设计」测评题并重新估计等级',
      dependsOn: null,
      done: false,
    },
    {
      id: 'task-002',
      title: '把缓存方案落到课程项目的商品详情接口',
      gapLabel: '应用差距 · Redis 缓存设计 L2 → L3',
      estimatedMinutes: 210,
      artifact: '项目仓库中可运行的商品详情接口改动，以及一份压测前后对比记录',
      acceptance: [
        '接口在缓存命中与未命中下返回结构一致',
        '人工制造一次缓存失效，接口仍能返回可用结果',
        '提交记录中说明改动原因',
      ],
      retest: '用同一份压测脚本复测，并记录命中率',
      dependsOn: 'task-001',
      done: false,
    },
    {
      id: 'task-003',
      title: 'MySQL 索引优化的复盘：把上周的慢查询记录整理成结论',
      gapLabel: '证据补齐 · MySQL 索引优化缺少可引用产物',
      estimatedMinutes: 90,
      artifact: '一份包含 EXPLAIN 前后对比的复盘记录',
      acceptance: ['至少包含 2 条真实慢查询', '每条给出索引调整前后的执行计划差异'],
      retest: '无需复测；产物将作为能力证据进入证据列表',
      dependsOn: null,
      done: true,
    },
    {
      id: 'task-004',
      title: 'Java 并发编程：线程池参数与拒绝策略',
      gapLabel: '知识差距 · Java 并发编程 未知 → L2',
      estimatedMinutes: 60,
      artifact: '线程池参数取值说明与一次拒绝策略实验记录',
      acceptance: ['说明核心线程数、队列容量与拒绝策略的关系', '记录一次触发拒绝策略的实验过程'],
      retest: '完成后进入测评，重新估计该项等级',
      dependsOn: 'task-002',
      done: false,
    },
  ] as DemoLearningTask[],
}

// ============================================================
// 简历建议（19 号文档 §7.7）
// ============================================================

export interface DemoResumeEvidence {
  /** 来源类型：必须用明确文本区分，不能只靠颜色（§6.2）。 */
  sourceType: '简历自述' | '测试回答' | '实践产物' | '人工确认'
  summary: string
  direction: 'SUPPORT' | 'WEAKEN' | 'NEUTRAL'
  confidence: number
  at: string
}

export interface DemoResumeAdvice {
  id: string
  section: string
  original: string
  suggestion: string
  reason: string
  evidence: DemoResumeEvidence
  /** 风险提示必须贴近具体建议，不能用全页泛化免责声明替代（§7.7）。 */
  risk: string | null
  /** 未确认内容不得直接覆盖原文（§7.7），因此初始状态一律为 PENDING。 */
  state: 'PENDING' | 'ACCEPTED' | 'EDITED' | 'IGNORED'
}

export const DEMO_RESUME_ADVICE: DemoResumeAdvice[] = [
  {
    id: 'advice-001',
    section: '项目经历 · 校园二手交易平台',
    original:
      '负责后端开发，使用 Spring Boot 与 MyBatis 完成接口开发，参与数据库表设计。',
    suggestion:
      '负责订单与商品模块的接口设计（12 个 REST 接口），基于 Spring Boot + MyBatis 实现；\n' +
      '针对商品列表查询引入组合索引，把 2 万条数据下的列表响应从约 600ms 降到约 120ms（附 EXPLAIN 前后对比）。',
    reason:
      '原文只有职责描述，没有可核查的规模与结果。岗位要求「熟悉 MySQL 索引优化」，需要一条能对应到该要求的量化产物。',
    evidence: {
      sourceType: '实践产物',
      summary: '项目仓库中的索引调整提交，以及一份 EXPLAIN 前后对比记录',
      direction: 'SUPPORT',
      confidence: 0.81,
      at: '2026-09-16',
    },
    risk: '数字必须来自你自己的压测记录。若无法复现 600ms / 120ms，请改成实际测量值或只保留「加入了组合索引」这一事实。',
    state: 'PENDING',
  },
  {
    id: 'advice-002',
    section: '技能清单',
    original: '熟悉 Redis，了解分布式。',
    suggestion: 'Redis：能说明缓存击穿 / 穿透 / 雪崩的失效路径与降级方案（测评 L2，置信度 78%，4 条证据）。',
    reason:
      '「熟悉 / 了解」没有等级信息，面试官无法判断真实水平。改成「能力 + 等级 + 证据」后，与你在能力图谱中登记的 L2 一致，不会出现简历与档案互相矛盾。',
    evidence: {
      sourceType: '测试回答',
      summary: '测评第 3 题作答：说明缓存粒度与过期策略，未覆盖缓存不可用时的降级',
      direction: 'WEAKEN',
      confidence: 0.78,
      at: '2026-09-21',
    },
    risk: '你当前尚未通过测评证明 L3，因此不建议直接写「精通 Redis」。若本周补齐 task-001、task-002 后可重新评估。',
    state: 'PENDING',
  },
  {
    id: 'advice-003',
    section: '教育背景',
    original: '2022.09 - 2026.06 某某大学 计算机科学与技术 本科',
    suggestion: '保持原文，不建议压缩。该岗位硬门槛明确要求「本科及以上」，教育背景需要完整可见。',
    reason: '该段已包含学历层次与专业，符合岗位硬门槛的可核查要求。',
    evidence: {
      sourceType: '人工确认',
      summary: '你在目标创建时登记学历为本科、毕业年份 2026',
      direction: 'SUPPORT',
      confidence: 0.9,
      at: '2026-09-14',
    },
    risk: null,
    state: 'ACCEPTED',
  },
]

/** 简历原文全文（用于左侧原文栏，§7.7 桌面并排对照）。 */
export const DEMO_RESUME_SECTIONS = [
  { key: 'edu', label: '教育背景', text: '2022.09 - 2026.06 某某大学 计算机科学与技术 本科' },
  {
    key: 'skills',
    label: '技能清单',
    text: '熟悉 Java、Spring Boot、MyBatis；熟悉 Redis，了解分布式；了解 MySQL 索引优化。',
  },
  {
    key: 'project',
    label: '项目经历 · 校园二手交易平台',
    text: '负责后端开发，使用 Spring Boot 与 MyBatis 完成接口开发，参与数据库表设计。',
  },
  { key: 'practice', label: '实践经历', text: '暂无（尚未填写）' },
]