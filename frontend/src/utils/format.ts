/**
 * 展示用格式化工具。
 *
 * 存在的理由：5 个页面都要把契约里的 ISO 时间、分钟数、比率转成中文界面文案。
 * 分散到各页面会造成同一字段在不同页面显示成不同格式（例如有的显示 "09-21"，
 * 有的显示完整时间），而 19-ui-design-system.md §2.3 要求统一结构优先于局部创意。
 *
 * 约束：
 *   - 时间缺失一律显示「未知」，不显示 "Invalid Date" 或空字符串；
 *   - 数字保留原始精度语义，不四舍五入成好看的值（§16 禁止假指标）。
 */

/** 完整时间：2026-09-21 09:30 */
export function formatDateTime(iso: string | null): string {
  if (!iso) return '未知'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '未知'
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 短日期：09-21 */
export function formatShortDate(iso: string | null): string {
  if (!iso) return '未知'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '未知'
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 分钟数 → 「3 小时」/「1 小时 30 分钟」/「45 分钟」 */
export function formatDuration(minutes: number): string {
  if (!Number.isFinite(minutes) || minutes <= 0) return '未知'
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) return `${rest} 分钟`
  if (rest === 0) return `${hours} 小时`
  return `${hours} 小时 ${rest} 分钟`
}

/**
 * 比率展示：必须同时给出分子与分母，避免出现无法核算的百分比（§9）。
 * 例：formatRatio(9, 14, '项') → "9 / 14 项（64%）"
 */
export function formatRatio(numerator: number, denominator: number, unit = ''): string {
  if (denominator <= 0) return '无法计算'
  const percent = Math.round((numerator / denominator) * 100)
  return `${numerator} / ${denominator}${unit ? ` ${unit}` : ''}（${percent}%）`
}

/** 置信度（0~1）→ 「78%」；null → 「未知」 */
export function formatConfidence(value: number | null): string {
  return value === null ? '未知' : `${Math.round(value * 100)}%`
}

/** 等级展示：3 → 「L3」；null → 「未知」 */
export function formatLevel(value: number | null): string {
  return value === null ? '未知' : `L${value}`
}