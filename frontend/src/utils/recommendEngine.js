// 内容推荐引擎（纯前端）
// 输入：{ health, mental }
//   health: { hasData, bmi, sleepAvg, stepsAvg, systolic, diastolic, heartRate }
//   mental: { hasData, latest:{ wellness, dep, anx, sleep, stress }, trend:{ isLongTermLow, recentAllLow } }
// 输出：{ needs:[需求画像], groups:[按需求分组的内容], total }
//
// 设计要点：
//   1. 先根据指标阈值生成「需求画像」，每条带权重(1~4)与触发原因。
//   2. 权重越高代表越需要被优先关注（如长期情绪低谷权重最高）。
//   3. 按需求标签到内容库匹配内容，跨需求去重（同一内容只出现在最相关的分组），
//      组内按 priority 降序展示。

import { CONTENT_LIBRARY } from '@/data/recommendContent'

// 需求元信息（icon 取 Element Plus 已注册图标名）
const NEED_META = {
  weight_high: { label: '体重偏高', icon: 'Basketball', color: '#E6A23C' },
  weight_low: { label: '体重偏轻', icon: 'Apple', color: '#909399' },
  sleep_low: { label: '睡眠不足', icon: 'Moon', color: '#409EFF' },
  exercise_low: { label: '运动不足', icon: 'Bicycle', color: '#67C23A' },
  relax: { label: '需放松减压', icon: 'Coffee', color: '#909399' },
  mood_low: { label: '情绪压力', icon: 'Cloudy', color: '#F56C6C' },
  stress_high: { label: '压力较大', icon: 'Lightning', color: '#E6A23C' },
  anxiety_high: { label: '焦虑明显', icon: 'Warning', color: '#F56C6C' },
}

export function buildRecommendations({ health, mental } = {}) {
  const needs = []
  const push = (key, weight, reason) => {
    const ex = needs.find((n) => n.key === key)
    if (ex) {
      // 同一需求被多指标触发时，取更高权重并补充原因
      if (weight > ex.weight) {
        ex.weight = weight
        ex.reason = reason
      }
      return
    }
    const meta = NEED_META[key]
    needs.push({ key, label: meta.label, icon: meta.icon, color: meta.color, weight, reason })
  }

  // —— 健康数据维度 ——
  if (health?.hasData) {
    const bmi = health.bmi
    if (bmi >= 28) push('weight_high', 3, `BMI ${bmi} 已达肥胖范围`)
    else if (bmi >= 24) push('weight_high', 2, `BMI ${bmi} 处于超重范围`)
    else if (bmi > 0 && bmi < 18.5) push('weight_low', 1, `BMI ${bmi} 偏瘦`)

    const s = health.sleepAvg
    if (s != null) {
      if (s < 6) push('sleep_low', 3, `近 7 天平均睡眠 ${s.toFixed(1)} 小时，严重不足`)
      else if (s < 7) push('sleep_low', 2, `近 7 天平均睡眠 ${s.toFixed(1)} 小时，略不足`)
    }

    const st = health.stepsAvg
    if (st != null && st < 8000) push('exercise_low', 2, `近 7 天平均步数 ${Math.round(st)}，运动偏少`)

    if (
      health.systolic >= 140 ||
      health.diastolic >= 90 ||
      (health.heartRate && health.heartRate > 100)
    ) {
      push('relax', 1, '血压或心率偏高，建议放松')
    }
  }

  // —— 心理测评维度 ——
  if (mental?.hasData) {
    const m = mental.latest
    if (mental.trend?.isLongTermLow || mental.trend?.recentAllLow) {
      push('mood_low', 4, '检测到长期/近期持续情绪低谷，建议重点疏导')
    } else if (m.wellness < 60) {
      push('mood_low', 3, `情绪健康分 ${m.wellness}，处于低落区间`)
    }
    if (m.stress >= 5) push('stress_high', 2, `压力维度分 ${m.stress}/9，压力较大`)
    if (m.anx >= 10) push('anxiety_high', 2, `GAD-7 焦虑分 ${m.anx}/21，焦虑明显`)
    if (m.sleep >= 5) push('sleep_low', 2, `睡眠维度分 ${m.sleep}/9，睡眠质量差`)
  }

  // 权重降序：越需要关注越靠前
  needs.sort((a, b) => b.weight - a.weight)

  // —— 匹配内容库（跨需求去重） ——
  const seen = new Set()
  const groups = []
  for (const need of needs) {
    const items = CONTENT_LIBRARY.filter(
      (c) => c.tags?.includes(need.key) && !seen.has(c.id),
    ).sort((a, b) => (b.priority || 0) - (a.priority || 0))
    items.forEach((i) => seen.add(i.id))
    if (items.length) groups.push({ ...need, items })
  }

  return {
    needs,
    groups,
    total: groups.reduce((sum, g) => sum + g.items.length, 0),
  }
}
