import type {
  ApiResponse,
  CapabilityMap,
  CreateGoalRequest,
  ErrorResponse,
  ImportJdRequest,
  JdImportResponse,
  JdParseResult,
  JobGoal,
} from './types'

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

/**
 * 统一的接口错误。
 * 携带后端 error.code / traceId，便于界面向用户展示可定位的问题编号。
 */
export class ApiError extends Error {
  readonly code: string
  readonly traceId: string
  readonly retryable: boolean
  readonly details: Record<string, unknown>

  constructor(
    code: string,
    message: string,
    traceId = '',
    retryable = false,
    details: Record<string, unknown> = {},
  ) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.traceId = traceId
    this.retryable = retryable
    this.details = details
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<ApiResponse<T>> {
  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init.body ? { 'Content-Type': 'application/json' } : {}),
        ...(init.headers ?? {}),
      },
    })
  } catch {
    throw new ApiError(
      'NETWORK_ERROR',
      '无法连接到后端服务，请确认服务已启动并检查网络。',
      '',
      true,
    )
  }

  const text = await response.text()
  let payload: unknown = null
  if (text) {
    try {
      payload = JSON.parse(text)
    } catch {
      throw new ApiError(
        'INVALID_RESPONSE',
        `后端返回了非 JSON 内容（HTTP ${response.status}）。`,
        '',
        response.status >= 500,
      )
    }
  }

  if (!response.ok) {
    const failure = payload as ErrorResponse | null
    throw new ApiError(
      failure?.error?.code ?? 'INTERNAL_ERROR',
      failure?.error?.message ?? `请求失败（HTTP ${response.status}）。`,
      failure?.traceId ?? '',
      failure?.error?.retryable ?? response.status >= 500,
      failure?.error?.details ?? {},
    )
  }

  return payload as ApiResponse<T>
}

export const api = {
  createGoal(body: CreateGoalRequest): Promise<ApiResponse<JobGoal>> {
    return request<JobGoal>('/api/v1/goals', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  listGoals(): Promise<ApiResponse<JobGoal[]>> {
    return request<JobGoal[]>('/api/v1/goals')
  },

  getGoal(goalId: string): Promise<ApiResponse<JobGoal>> {
    return request<JobGoal>(`/api/v1/goals/${encodeURIComponent(goalId)}`)
  },

  importJd(goalId: string, body: ImportJdRequest): Promise<ApiResponse<JdImportResponse>> {
    return request<JdImportResponse>(`/api/v1/goals/${encodeURIComponent(goalId)}/jd`, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  analyzeJd(goalId: string): Promise<ApiResponse<JdParseResult>> {
    return request<JdParseResult>(`/api/v1/goals/${encodeURIComponent(goalId)}/analyze`, {
      method: 'POST',
    })
  },

  getCapabilityMap(goalId: string): Promise<ApiResponse<CapabilityMap>> {
    return request<CapabilityMap>(
      `/api/v1/goals/${encodeURIComponent(goalId)}/capability-map`,
    )
  },
}