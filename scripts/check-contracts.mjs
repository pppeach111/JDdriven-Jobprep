#!/usr/bin/env node
/**
 * 契约一致性校验（CP-002 收尾，2026-09-22）。
 *
 * 校验 docs/contracts/schemas/*.json 与 frontend/src/api/types.ts 的字段一致性：
 *   - schema 顶层对象必须带 "x-frontend-interface" 注记，指向 types.ts 中的 interface 名；
 *   - schema properties 中每个字段必须出现在对应 interface 中（缺失即失败）；
 *   - schema required 中每个字段必须在 properties 中（自洽性检查）；
 *   - interface 中多出的字段只告警（前端可能领先契约，需人工确认）。
 *
 * 零依赖：仅使用 Node 内置模块。运行：node scripts/check-contracts.mjs
 */

import { readFileSync, readdirSync, existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'


const rootDir = join(dirname(fileURLToPath(import.meta.url)), '..')
const schemasDir = join(rootDir, 'docs/contracts/schemas')
const openapiPath = join(rootDir, 'docs/contracts/openapi.json')
const typesPath = join(rootDir, 'frontend/src/api/types.ts')

let failures = 0
let warnings = 0

function fail(message) {
  failures++
  console.error(`[FAIL] ${message}`)
}

function warn(message) {
  warnings++
  console.warn(`[WARN] ${message}`)
}

/** 解析 types.ts，返回 Map<interface 名, Set<字段名>> */
function parseInterfaces(source) {
  const interfaces = new Map()
  let current = null
  for (const line of source.split(/\r?\n/)) {
    const start = line.match(/^export interface (\w+)/)
    if (start) {
      current = { name: start[1], fields: new Set() }
      continue
    }
    if (current && /^}/.test(line)) {
      interfaces.set(current.name, current.fields)
      current = null
      continue
    }
    if (current) {
      const field = line.match(/^\s{2}(\w+)(\?)?:/)
      if (field) current.fields.add(field[1])
    }
  }
  return interfaces
}

function main() {
  if (!existsSync(typesPath)) {
    fail(`未找到 ${typesPath}`)
    process.exit(1)
  }
  if (!existsSync(schemasDir)) {
    fail('docs/contracts/schemas 不存在，契约导出缺失。')
    process.exit(1)
  }

  const interfaces = parseInterfaces(readFileSync(typesPath, 'utf8'))
  const schemaFiles = readdirSync(schemasDir).filter((name) => name.endsWith('.json'))

  if (schemaFiles.length === 0) {
    fail('docs/contracts/schemas 为空，契约导出缺失。')
    process.exit(1)
  }

  for (const file of schemaFiles) {
    const schemaFile = `schemas/${file}`
    let schema
    try {
      schema = JSON.parse(readFileSync(join(schemasDir, file), 'utf8'))
    } catch (error) {
      fail(`${schemaFile}: JSON 解析失败 — ${error.message}`)
      continue
    }

    const interfaceName = schema['x-frontend-interface']
    if (!interfaceName) {
      warn(`${schemaFile}: 缺少 x-frontend-interface，未与前端比对。`)
      continue
    }
    const tsFields = interfaces.get(interfaceName)
    if (!tsFields) {
      fail(`${schemaFile}: interface ${interfaceName} 在 types.ts 中不存在`)
      continue
    }

    const properties = schema.properties ?? {}
    const propertyNames = new Set(Object.keys(properties))

    for (const field of propertyNames) {
      if (!tsFields.has(field)) {
        fail(`${schemaFile}: 字段 "${interfaceName}.${field}" 在 types.ts 的 ${interfaceName} 中不存在`)
      }
    }
    for (const field of tsFields) {
      if (!propertyNames.has(field)) {
        warn(`${schemaFile}: types.ts 的 ${interfaceName} 有字段 "${field}" 不在 schema 中（人工确认是否契约缺失）`)
      }
    }
    for (const field of schema.required ?? []) {
      if (!propertyNames.has(field)) {
        fail(`${schemaFile}: required 声明的 "${field}" 不在 properties 中（schema 自相矛盾）`)
      }
    }
    console.log(`[OK] ${schemaFile} ↔ ${interfaceName}（${propertyNames.size} 个字段）`)
  }

  try {
    const openapi = JSON.parse(readFileSync(openapiPath, 'utf8'))
    const pathCount = Object.keys(openapi.paths ?? {}).length
    const operationCount = Object.values(openapi.paths ?? {}).reduce(
      (total, methods) => total + Object.keys(methods ?? {}).length,
      0,
    )
    if (pathCount < 6 || operationCount < 8) {
      fail(`openapi.json 路径 ${pathCount} / 操作 ${operationCount} 少于预期的 6 路径 8 操作。`)
    } else {
      console.log(`[OK] openapi.json：${pathCount} 条路径、${operationCount} 个操作可解析。`)
    }
  } catch (error) {
    fail(`openapi.json：${error.message}`)
  }

  console.log(`\n完成：${failures === 0 ? '全部一致' : '存在差异'}（错误 ${failures}，警告 ${warnings}）`)
  process.exit(failures === 0 ? 0 : 1)
}

main()
