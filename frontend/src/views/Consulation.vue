<template>
  <div class="ai-container">
    <div class="header">
      <h2>AI 健康建议</h2>
      <el-button type="primary" :loading="loading" @click="generateAdvice">
        {{ loading ? 'AI 正在分析中...' : '生成新建议' }}
      </el-button>
    </div>

    <!-- 本次建议：身体健康数据 -->
    <el-card v-if="basedOn" class="data-card" shadow="never">
      <template #header>
        <span>📊 本次建议基于以下身体健康数据（{{ basedOn.recordDate }}）</span>
      </template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="身高">{{ basedOn.height }} cm</el-descriptions-item>
        <el-descriptions-item label="体重">{{ basedOn.weight }} kg</el-descriptions-item>
        <el-descriptions-item label="BMI">{{ basedOn.bmi }}</el-descriptions-item>
        <el-descriptions-item label="血压">
          {{ basedOn.systolicPressure }}/{{ basedOn.diastolicPressure }} mmHg
        </el-descriptions-item>
        <el-descriptions-item label="心率">{{ basedOn.heartRate }} 次/分</el-descriptions-item>
        <el-descriptions-item label="步数">{{ basedOn.steps }} 步</el-descriptions-item>
        <el-descriptions-item label="睡眠">{{ basedOn.sleepHours }} 小时</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 本次建议：心理健康测评数据 -->
    <el-card v-if="basedOnMental && basedOnMental.hasData" class="data-card" shadow="never">
      <template #header>
        <span>🧠 本次建议同时参考了以下心理健康测评（{{ basedOnMental.date }}）</span>
      </template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="情绪健康分">{{ basedOnMental.wellness }} / 100</el-descriptions-item>
        <el-descriptions-item label="抑郁(PHQ-9)">{{ basedOnMental.dep }} 分</el-descriptions-item>
        <el-descriptions-item label="焦虑(GAD-7)">{{ basedOnMental.anx }} 分</el-descriptions-item>
        <el-descriptions-item label="睡眠维度">{{ basedOnMental.sleep }} 分</el-descriptions-item>
        <el-descriptions-item label="压力维度">{{ basedOnMental.stress }} 分</el-descriptions-item>
        <el-descriptions-item label="长期情绪低谷">
          <el-tag :type="basedOnMental.isLongTermLow ? 'danger' : 'success'" size="small">
            {{ basedOnMental.isLongTermLow ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="advice" class="advice-card" shadow="hover">
      <template #header>
        <div class="advice-card-header">
          <span>💡 本次 AI 健康建议</span>
          <div class="advice-card-actions">
            <el-button
              size="small"
              @click="goChat"
            >
              💬 与AI对话
            </el-button>
            <el-button
              size="small"
              type="success"
              :loading="importing"
              :disabled="currentAdviceId && importedIds[currentAdviceId]"
              @click="importToPlan(advice, currentAdviceId)"
            >
              {{ currentAdviceId && importedIds[currentAdviceId] ? '✓ 已导入计划' : '📥 一键导入制定计划' }}
            </el-button>
          </div>
        </div>
      </template>
      <div class="advice-content" v-html="formattedAdvice"></div>
    </el-card>

    <el-skeleton v-if="loading" :rows="6" animated style="margin-top: 20px" />

    <!-- 👇 历史记录 -->
    <el-card class="history-card" shadow="never" style="margin-top: 20px">
      <template #header>
        <div class="history-header">
          <span>📜 历史建议记录（共 {{ historyList.length }} 条）</span>
          <el-button size="small" @click="loadHistory">刷新</el-button>
        </div>
      </template>

      <el-empty v-if="historyList.length === 0" description="暂无历史建议" :image-size="80" />

      <el-timeline v-else>
        <el-timeline-item
          v-for="item in historyList"
          :key="item.id"
          :timestamp="formatTime(item.createTime)"
          placement="top"
          type="primary"
        >
          <el-card shadow="hover" class="history-item">
            <div class="history-meta">
              <el-tag size="small">基于 {{ item.recordDate }}</el-tag>
              <div class="history-actions">
                <el-button
                  link
                  type="success"
                  size="small"
                  :disabled="importedIds[item.id]"
                  @click="importToPlan(item.adviceContent, item.id)"
                >
                  {{ importedIds[item.id] ? '✓ 已导入' : '导入计划' }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="handleDelete(item.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
            <div class="advice-content" v-html="formatAdvice(item.adviceContent)"></div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getHistory, analyzeTrend } from '@/utils/mentalHealth'
import { addPlan } from '@/api/healthPlan'

const router = useRouter()

const loading = ref(false)
const advice = ref('')
const currentAdviceId = ref(null)
const basedOn = ref(null)
const basedOnMental = ref(null)
const historyList = ref([])
const importing = ref(false)
// 记录已导入的建议 id，避免重复导入
const importedIds = reactive({})

// 由本地心理测评记录构造传给后端的摘要
function buildMentalPayload() {
  const history = getHistory()
  if (!history || !history.length) return null
  const latest = [...history].sort(
    (a, b) => new Date(b.date) - new Date(a.date),
  )[0]
  const trend = analyzeTrend(history)
  return {
    hasData: true,
    date: latest.date,
    wellness: latest.wellness,
    dep: latest.dep,
    anx: latest.anx,
    sleep: latest.sleep,
    stress: latest.stress,
    isLongTermLow: !!(trend && trend.isLongTermLow),
  }
}

// 生成新建议（同时把心理测评数据一并提交给 AI）
const generateAdvice = async () => {
  loading.value = true
  advice.value = ''
  basedOnMental.value = null
  try {
    const mental = buildMentalPayload()
    const res = await request.post(
      '/ai/health-advice',
      mental ? { mentalHealth: mental } : {},
    )
    advice.value = res.advice
    currentAdviceId.value = res.id || null
    basedOn.value = res.basedOn
    basedOnMental.value = res.basedOnMental || (mental ? mental : null)
    ElMessage.success('生成成功')
    loadHistory()   // 👈 生成后立即刷新历史记录
  } catch (error) {
    console.error('AI 生成失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载历史记录
const loadHistory = async () => {
  try {
    const res = await request.post('/ai/advice-history')
    historyList.value = res || []
  } catch (error) {
    console.error('加载历史失败:', error)
  }
}

// 删除历史记录
const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除这条建议吗？', '提示', { type: 'warning' })
    .then(async () => {
      await request.post(`/ai/advice-del?id=${id}`)
      ElMessage.success('删除成功')
      loadHistory()
    })
    .catch(() => {})
}

// 格式化建议文本
const formatAdvice = (text) => {
  if (!text) return ''
  return text
    .replace(/\n/g, '<br/>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}

// ========== 一键导入「制定计划」 ==========
// 1) 把建议文本按分点/标题切片成多条计划
function parseAdviceToPlans(text) {
  if (!text) return []
  const clean = (s) => s.replace(/\*\*/g, '').replace(/^[\d]+[.、]/, '').replace(/^[#*\-—•\s]+/, '').trim()
  const lines = text.split(/\n+/).map((l) => l.trim()).filter(Boolean)
  const blocks = []
  let cur = null
  const isHeading = (l) =>
    /^(\d+[.、]|\*|[-—•]|\*\*|#)/.test(l) || /[：:]\s*$/.test(l)
  for (const line of lines) {
    if (isHeading(line) && cur) {
      blocks.push(cur)
      cur = [line]
    } else if (!cur) {
      cur = [line]
    } else {
      cur.push(line)
    }
  }
  if (cur) blocks.push(cur)
  const items = blocks
    .map((b) => ({
      title: clean(b[0]),
      desc: b.map(clean).join('\n'),
    }))
    .filter((it) => it.desc)
  return items.slice(0, 8)
}

// 2) 根据关键词推断计划类型
function deriveType(text) {
  const t = text || ''
  if (/运动|锻炼|跑步|健身|步数|有氧|瑜伽|拉伸|减脂|力量|爬楼/.test(t)) return '运动'
  if (/饮食|营养|吃|碳水|蛋白|热量|三餐|喝水|水分|减脂餐|控糖/.test(t)) return '饮食'
  if (/睡眠|早睡|作息|失眠|助眠|午睡|褪黑/.test(t)) return '睡眠'
  return '综合'
}

// 3) 组装成后端 HealthPlan 结构（日期默认今天~14天后，提醒 09:00）
function buildPlans(text) {
  const items = parseAdviceToPlans(text)
  const today = new Date()
  const end = new Date(today)
  end.setDate(end.getDate() + 14)
  const fmt = (d) => d.toISOString().slice(0, 10)
  return items.map((it) => ({
    planName: it.title.length > 20 ? it.title.slice(0, 20) + '…' : it.title || 'AI 健康建议',
    planType: deriveType(it.title + it.desc),
    description: it.desc,
    startDate: fmt(today),
    endDate: fmt(end),
    remindTime: '09:00:00',
    status: '进行中',
  }))
}

// 4) 一键入库
const importToPlan = async (text, id) => {
  if (id && importedIds[id]) return
  const plans = buildPlans(text)
  if (!plans.length) return ElMessage.warning('当前建议内容暂无法解析为计划')
  importing.value = true
  try {
    let ok = 0
    for (const p of plans) {
      await addPlan(p)
      ok++
    }
    if (id) importedIds[id] = true
    ElMessage.success(`已导入 ${ok} 条计划到「制定计划」`)
  } catch (e) {
    console.error('导入失败:', e)
    ElMessage.error('导入失败，请稍后重试')
  } finally {
    importing.value = false
  }
}

const formattedAdvice = computed(() => formatAdvice(advice.value))

// 跳转到与 AI 对话页面
const goChat = () => {
  router.push('/back/ai-chat')
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return String(time).replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.ai-container { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.data-card, .advice-card, .history-card { margin-bottom: 20px; }
.advice-content {
  line-height: 1.8;
  font-size: 15px;
  color: #303133;
  white-space: pre-wrap;
}
/* AI 建议卡片头部：标题 + 导入按钮 右对齐 */
.advice-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.advice-card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.history-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.history-header { display: flex; justify-content: space-between; align-items: center; }
.history-item { margin-bottom: 10px; }
.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
</style>
