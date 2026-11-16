// 心理健康测评 —— 题库、评分、历史存储与长期情绪低谷监测
// 指标依据国际通用筛查量表设计：
//   - PHQ-9（患者健康问卷-9）：抑郁倾向筛查，9 题，分值 0~27
//   - GAD-7（广泛性焦虑量表-7）：焦虑倾向筛查，7 题，分值 0~21
//   - 睡眠 / 压力：作为抑郁、焦虑的常见共病与诱发因素补充评估

// 每道题采用 0~3 的频率计分（近两周内）：
//   0 = 完全没有   1 = 有几天   2 = 一半以上天数   3 = 几乎每天
export const FREQUENCY_OPTIONS = [
  { label: '完全没有', value: 0 },
  { label: '有几天', value: 1 },
  { label: '一半以上天数', value: 2 },
  { label: '几乎每天', value: 3 },
]

// 测评维度与题目（面向用户的中文表述）
export const DIMENSIONS = [
  {
    key: 'depression',
    title: '情绪心境（抑郁倾向）',
    desc: '参考 PHQ-9 抑郁症筛查量表 · 共 9 题',
    questions: [
      '做事时提不起劲或没有兴趣',
      '感到心情低落、沮丧或绝望',
      '入睡困难、睡不安稳，或睡眠过多',
      '感觉疲倦或没有活力',
      '食欲不振，或吃得过多',
      '觉得自己很糟，或觉得自己很失败，或让自己、家人失望',
      '注意力难以集中，例如读报或看电视时',
      '行动或说话明显比平时缓慢；或正好相反——坐立不安、动来动去',
      '有不如死掉或用某种方式伤害自己的念头',
    ],
  },
  {
    key: 'anxiety',
    title: '焦虑紧张（焦虑倾向）',
    desc: '参考 GAD-7 广泛性焦虑筛查量表 · 共 7 题',
    questions: [
      '感到紧张、焦虑或急切',
      '不能停止或控制担忧',
      '对各种各样的事情担忧过多',
      '很难放松下来',
      '由于不安而难以静坐',
      '变得容易烦恼或急躁',
      '感到似乎将有可怕的事情发生而害怕',
    ],
  },
  {
    key: 'sleep',
    title: '睡眠质量',
    desc: '评估近两周的睡眠状况 · 共 3 题',
    questions: [
      '睡眠质量差，入睡困难或夜间频繁醒来',
      '睡眠时间明显不足（平均每天不足 6 小时）',
      '白天因睡眠问题感到困倦、精力不足',
    ],
  },
  {
    key: 'stress',
    title: '压力与活力',
    desc: '评估压力感知与日常活力 · 共 3 题',
    questions: [
      '感到压力很大，难以应对日常事务',
      '对以往喜欢的事情失去兴趣',
      '感到孤独，或觉得缺乏支持',
    ],
  },
]

// 题量统计
export const TOTAL_QUESTIONS = DIMENSIONS.reduce(
  (sum, d) => sum + d.questions.length,
  0,
)

// 各维度满分（用于结果展示）
export const DIMENSION_MAX = {
  depression: 27,
  anxiety: 21,
  sleep: 9,
  stress: 9,
}

// ============ 评分逻辑 ============

const sum = (arr) => (arr || []).reduce((a, b) => a + (b || 0), 0)

// 抑郁分级（PHQ-9 标准）
export function depressionLevel(score) {
  if (score <= 4) return { text: '无明显抑郁症状', type: 'success' }
  if (score <= 9) return { text: '轻度抑郁', type: 'warning' }
  if (score <= 14) return { text: '中度抑郁', type: 'danger' }
  if (score <= 19) return { text: '中重度抑郁', type: 'danger' }
  return { text: '重度抑郁', type: 'danger' }
}

// 焦虑分级（GAD-7 标准）
export function anxietyLevel(score) {
  if (score <= 4) return { text: '无明显焦虑', type: 'success' }
  if (score <= 9) return { text: '轻度焦虑', type: 'warning' }
  if (score <= 14) return { text: '中度焦虑', type: 'danger' }
  return { text: '重度焦虑', type: 'danger' }
}

// 总体情绪健康分（0~100，越高越好）与状态判定
function overallState(wellness) {
  if (wellness >= 80) return { text: '状态良好', type: 'success' }
  if (wellness >= 60) return { text: '轻度低落，建议自我调节', type: 'warning' }
  if (wellness >= 40) return { text: '明显低落，建议关注', type: 'danger' }
  return { text: '重度低落，建议尽快求助', type: 'danger' }
}

function adviceByState(wellness) {
  if (wellness >= 80)
    return [
      '保持规律作息、适度运动与社交联系',
      '可每月自评一次，跟踪情绪变化趋势',
    ]
  if (wellness >= 60)
    return [
      '增加户外活动与日照，规律睡眠',
      '与亲友倾诉，减少独处与过度反刍',
      '若低落状态持续超过两周，请开始关注',
    ]
  if (wellness >= 40)
    return [
      '尝试正念、放松训练或规律运动缓解情绪',
      '如已影响工作/生活，建议预约心理咨询',
      '保持与可信赖的人沟通，避免孤立',
    ]
  return [
    '强烈建议尽快联系专业心理或精神科医生',
    '请勿独自承受，主动寻求家人、朋友陪伴',
    '如出现自伤念头，请立即拨打援助热线',
  ]
}

// answers: { depression:[...9], anxiety:[...7], sleep:[...3], stress:[...3] }
export function computeResult(answers) {
  const dep = sum(answers.depression)
  const anx = sum(answers.anxiety)
  const sleep = sum(answers.sleep)
  const stress = sum(answers.stress)
  const totalRaw = dep + anx + sleep + stress

  // 抑郁、焦虑为主维度（权重 2），睡眠、压力为辅维度（权重 1）
  const weighted = 2 * dep + 2 * anx + 1 * sleep + 1 * stress
  const maxWeighted = 2 * 27 + 2 * 21 + 9 + 9 // = 114
  const wellness = Math.max(0, Math.min(100, Math.round((1 - weighted / maxWeighted) * 100)))

  const state = overallState(wellness)
  const depLevel = depressionLevel(dep)
  const anxLevel = anxietyLevel(anx)
  // 自伤念头（PHQ-9 第 9 题）为高危信号
  const selfHarm = (answers.depression?.[8] || 0) > 0

  return {
    wellness,
    state,
    dep,
    anx,
    sleep,
    stress,
    totalRaw,
    depLevel,
    anxLevel,
    selfHarm,
    advice: adviceByState(wellness),
  }
}

// ============ 本地存储（按用户隔离） ============

const STORAGE_KEY = 'hm_mental_health_records'

function getUserId() {
  const u = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return u.userId || u.id || 'default'
}

export function getHistory() {
  const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  return all[getUserId()] || []
}

export function saveRecord(record) {
  const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  const uid = getUserId()
  if (!all[uid]) all[uid] = []
  all[uid].push(record)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  return all[uid]
}

export function deleteRecord(id) {
  const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  const uid = getUserId()
  if (!all[uid]) return []
  all[uid] = all[uid].filter((r) => r.id !== id)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  return all[uid]
}

export function clearHistory() {
  const all = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  const uid = getUserId()
  all[uid] = []
  localStorage.setItem(STORAGE_KEY, JSON.stringify(all))
  return []
}

// ============ 长期情绪低谷监测 ============

// 输入历史记录数组，返回长期趋势分析
// 判定“长期情绪低谷”：
//   1) 至少 3 次测评
//   2) 时间跨度 ≥ 14 天
//   3) 其中 ≥ 50% 的测评情绪健康分 < 60（处于低落区间）
export function analyzeTrend(records) {
  if (!records || records.length < 2) {
    return { enough: false, message: '完成至少 2 次测评后，即可查看情绪趋势与长期监测' }
  }
  const sorted = [...records].sort((a, b) => new Date(a.date) - new Date(b.date))
  const dates = sorted.map((r) => new Date(r.date))
  const spanDays = Math.round((dates[dates.length - 1] - dates[0]) / 86400000)
  const lowList = sorted.filter((r) => r.wellness < 60)
  const lowRatio = lowList.length / sorted.length

  // 最近 3 次连续低落（早期预警）
  const recent = sorted.slice(-3)
  const recentAllLow = recent.length === 3 && recent.every((r) => r.wellness < 60)

  const isLongTermLow =
    sorted.length >= 3 && spanDays >= 14 && lowRatio >= 0.5

  return {
    enough: true,
    spanDays,
    total: sorted.length,
    lowCount: lowList.length,
    lowRatio: Math.round(lowRatio * 100),
    recentAllLow,
    isLongTermLow,
    sorted,
  }
}
