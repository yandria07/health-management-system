<template>
  <div class="mh-container">
    <!-- 顶部说明与免责声明 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="disclaimer"
    >
      <template #title>心理健康自评 · 情绪低谷监测</template>
      <span>
        本测评参考 PHQ-9、GAD-7 等国际通用筛查量表，用于自我情绪监测与长期追踪，<b>不构成医学诊断</b>。
        如持续情绪低落或出现自伤念头，请尽快联系专业机构。心理援助热线：全国 24 小时
        <b>400-161-9995</b> / 北京 010-82951332。
      </span>
    </el-alert>

    <el-row :gutter="20">
      <!-- 左侧：测评问卷 -->
      <el-col :xs="24" :lg="13">
        <el-card shadow="never" class="panel">
          <div class="panel-head">
            <h2>情绪状态测评</h2>
            <el-progress
              :percentage="progressPercent"
              :stroke-width="10"
              style="width: 180px"
            />
          </div>
          <p class="hint">请回顾<b>最近两周</b>的真实感受，选择最符合的频率：</p>

          <el-collapse v-model="activeNames" class="q-collapse">
            <el-collapse-item
              v-for="dim in DIMENSIONS"
              :key="dim.key"
              :name="dim.key"
            >
              <template #title>
                <span class="dim-title">{{ dim.title }}</span>
                <span class="dim-desc">{{ dim.desc }}</span>
              </template>
              <div
                v-for="(q, qi) in dim.questions"
                :key="qi"
                class="question"
              >
                <div class="q-text">
                  <span class="q-index">{{ qi + 1 }}.</span>{{ q }}
                </div>
                <el-radio-group
                  v-model="answers[dim.key][qi]"
                  size="small"
                  @change="updateProgress"
                >
                  <el-radio-button
                    v-for="opt in FREQUENCY_OPTIONS"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </el-radio-button>
                </el-radio-group>
              </div>
            </el-collapse-item>
          </el-collapse>

          <div class="actions">
            <el-button @click="resetForm">重置</el-button>
            <el-button
              type="primary"
              :disabled="progressPercent < 100"
              @click="submitAssessment"
            >
              提交并保存测评
            </el-button>
          </div>
          <p v-if="progressPercent < 100" class="warn-text">
            还有 {{ TOTAL_QUESTIONS - answeredCount }} 题未作答。
          </p>
        </el-card>
      </el-col>

      <!-- 右侧：本次结果 -->
      <el-col :xs="24" :lg="11">
        <el-card v-if="result" shadow="never" class="panel result-panel">
          <div class="panel-head">
            <h2>本次测评结果</h2>
            <span class="result-date">{{ currentRecord.date }}</span>
          </div>

          <!-- 自伤高危提示 -->
          <el-alert
            v-if="result.selfHarm"
            type="error"
            :closable="false"
            show-icon
            class="self-harm"
          >
            <template #title>请重视：你提到了伤害自己的念头</template>
            你并非孤身一人。请立即联系可信赖的人，或拨打心理援助热线
            <b>400-161-9995</b>（24 小时）。如有紧急危险，请拨打 120。
          </el-alert>

          <!-- 情绪健康分 -->
          <div class="score-wrap">
            <el-progress
              type="dashboard"
              :percentage="result.wellness"
              :color="scoreColor"
              :width="160"
            >
              <template #default="{ percentage }">
                <span class="score-num">{{ percentage }}</span>
                <span class="score-label">情绪健康分</span>
              </template>
            </el-progress>
            <el-tag :type="result.state.type" size="large" class="state-tag">
              {{ result.state.text }}
            </el-tag>
          </div>

          <!-- 维度明细 -->
          <el-descriptions :column="1" border class="dim-detail">
            <el-descriptions-item label="抑郁倾向">
              <span>{{ result.dep }} / 27</span>
              <el-tag :type="result.depLevel.type" size="small" style="margin-left: 8px">
                {{ result.depLevel.text }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="焦虑倾向">
              <span>{{ result.anx }} / 21</span>
              <el-tag :type="result.anxLevel.type" size="small" style="margin-left: 8px">
                {{ result.anxLevel.text }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="睡眠质量">
              {{ result.sleep }} / 9
            </el-descriptions-item>
            <el-descriptions-item label="压力与活力">
              {{ result.stress }} / 9
            </el-descriptions-item>
          </el-descriptions>

          <!-- 建议 -->
          <div class="advice">
            <div class="advice-title">建议</div>
            <ul>
              <li v-for="(a, i) in result.advice" :key="i">{{ a }}</li>
            </ul>
          </div>
        </el-card>

        <!-- 长期情绪低谷监测面板 -->
        <el-card shadow="never" class="panel monitor-panel">
          <div class="panel-head">
            <h2>长期情绪监测</h2>
            <el-tag v-if="trend.enough" type="info" size="small">
              {{ trend.total }} 次 · 跨度 {{ trend.spanDays }} 天
            </el-tag>
          </div>

          <el-alert
            v-if="trend.isLongTermLow"
            type="error"
            :closable="false"
            show-icon
            class="monitor-alert"
          >
            <template #title>⚠ 长期情绪低谷预警</template>
            你近 {{ trend.spanDays }} 天内共有 {{ trend.total }} 次测评，
            其中 <b>{{ trend.lowCount }} 次（{{ trend.lowRatio }}%）</b>处于情绪低落区间。
            数据显示你可能正处于<b>长期情绪低谷期</b>，建议尽快寻求专业帮助，并联系身边支持系统。
          </el-alert>

          <el-alert
            v-else-if="trend.recentAllLow"
            type="warning"
            :closable="false"
            show-icon
            class="monitor-alert"
          >
            <template #title>近期情绪低落预警</template>
            你最近 3 次测评均处于低落区间，请关注情绪变化，必要时寻求支持。
          </el-alert>

          <el-alert
            v-else-if="trend.enough"
            type="success"
            :closable="false"
            show-icon
            class="monitor-alert"
          >
            <template #title>暂未检测到长期低谷</template>
            目前数据未见持续低落趋势，继续保持关注与定期自评。
          </el-alert>

          <p v-else class="empty-hint">{{ trend.message }}</p>

          <div ref="trendRef" class="trend-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史记录 -->
    <el-card shadow="never" class="panel history-panel">
      <div class="panel-head">
        <h2>历史测评记录</h2>
        <el-button
          v-if="history.length"
          type="danger"
          plain
          size="small"
          @click="handleClear"
        >
          清空记录
        </el-button>
      </div>

      <el-empty v-if="!history.length" description="暂无测评记录" />
      <el-table v-else :data="history" style="width: 100%">
        <el-table-column prop="date" label="测评日期" width="160" />
        <el-table-column label="情绪健康分" width="120">
          <template #default="{ row }">
            <el-tag :type="scoreTagType(row.wellness)">{{ row.wellness }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dep" label="抑郁" width="90" />
        <el-table-column prop="anx" label="焦虑" width="90" />
        <el-table-column prop="sleep" label="睡眠" width="90" />
        <el-table-column prop="stress" label="压力" width="90" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag :type="row.stateType">{{ row.stateText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DIMENSIONS,
  FREQUENCY_OPTIONS,
  TOTAL_QUESTIONS,
  computeResult,
  getHistory,
  saveRecord,
  deleteRecord,
  clearHistory,
  analyzeTrend,
} from '@/utils/mentalHealth'

// 默认全部折叠（不展开任何测评维度），呈现与截图一致的收起样式；用户点击标题时再展开
const activeNames = ref([])

// 初始化答案结构：每个维度一个长度等于题数的数组，默认 null
const answers = reactive(
  DIMENSIONS.reduce((acc, d) => {
    acc[d.key] = Array(d.questions.length).fill(null)
    return acc
  }, {}),
)

const answeredCount = computed(() => {
  let n = 0
  DIMENSIONS.forEach((d) => {
    answers[d.key].forEach((v) => {
      if (v !== null && v !== undefined) n++
    })
  })
  return n
})
const progressPercent = computed(() =>
  Math.round((answeredCount.value / TOTAL_QUESTIONS) * 100),
)

const result = ref(null)
const currentRecord = ref({})
const history = ref([])
const trend = ref({ enough: false, message: '' })
const trendRef = ref(null)
let trendChart = null

const scoreColor = computed(() => {
  if (!result.value) return '#409EFF'
  const w = result.value.wellness
  if (w >= 80) return '#67C23A'
  if (w >= 60) return '#E6A23C'
  return '#F56C6C'
})

const updateProgress = () => {}

function resetForm() {
  DIMENSIONS.forEach((d) => {
    answers[d.key] = Array(d.questions.length).fill(null)
  })
  result.value = null
  currentRecord.value = {}
}

function submitAssessment() {
  if (answeredCount.value < TOTAL_QUESTIONS) {
    return ElMessage.warning('请完成全部题目后再提交')
  }
  const res = computeResult(answers)
  const now = new Date()
  const record = {
    id: `${now.getTime()}`,
    date: now.toLocaleString('zh-CN', { hour12: false }),
    wellness: res.wellness,
    dep: res.dep,
    anx: res.anx,
    sleep: res.sleep,
    stress: res.stress,
    stateText: res.state.text,
    stateType: res.state.type,
    answers: JSON.parse(JSON.stringify(answers)),
  }
  history.value = saveRecord(record)
  result.value = res
  currentRecord.value = record
  refreshTrend()
  ElMessage.success('测评已保存')
}

function scoreTagType(w) {
  if (w >= 80) return 'success'
  if (w >= 60) return 'warning'
  return 'danger'
}

function handleDelete(id) {
  ElMessageBox.confirm('确定删除该条测评记录吗？', '提示', {
    type: 'warning',
  }).then(() => {
    history.value = deleteRecord(id)
    refreshTrend()
    ElMessage.success('已删除')
  })
}

function handleClear() {
  ElMessageBox.confirm('将清空全部测评历史，无法恢复。确定吗？', '警告', {
    type: 'warning',
  }).then(() => {
    history.value = clearHistory()
    result.value = null
    currentRecord.value = {}
    refreshTrend()
    ElMessage.success('已清空')
  })
}

// 重新计算趋势并刷新图表
function refreshTrend() {
  trend.value = analyzeTrend(history.value)
  nextTick(renderTrendChart)
}

function renderTrendChart() {
  if (!trendRef.value) return
  const data = trend.value.sorted || []
  if (!data.length) {
    trendChart?.clear()
    return
  }
  const dates = data.map((r) => r.date.split(' ')[0])
  const scores = data.map((r) => r.wellness)
  if (!trendChart) trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    backgroundColor: '#fafcff',
    tooltip: { trigger: 'axis' },
    grid: { left: '8%', right: '5%', top: 40, bottom: 30, containLabel: true },
    title: { text: '情绪健康分趋势', left: 'center', textStyle: { fontSize: 14 } },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value', min: 0, max: 100, name: '分' },
    series: [
      {
        name: '情绪健康分',
        type: 'line',
        data: scores,
        smooth: true,
        symbolSize: 8,
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.35)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' },
          ]),
        },
        markLine: {
          silent: true,
          symbol: 'none',
          data: [
            { yAxis: 60, lineStyle: { color: '#E6A23C', type: 'dashed' }, label: { formatter: '低落临界 60' } },
          ],
        },
      },
    ],
  })
}

const handleResize = () => trendChart?.resize()

onMounted(() => {
  history.value = getHistory()
  refreshTrend()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.mh-container {
  padding: 4px;
}
.disclaimer {
  margin-bottom: 16px;
  line-height: 1.7;
}
.panel {
  margin-bottom: 20px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.panel-head h2 {
  font-size: 18px;
  margin: 0;
  color: #303133;
}
.hint {
  color: #909399;
  font-size: 13px;
  margin: 0 0 12px;
}
.q-collapse {
  border: none;
}
.dim-title {
  font-weight: 600;
  color: #303133;
}
.dim-desc {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}
.question {
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
}
.question:last-child {
  border-bottom: none;
}
.q-text {
  margin-bottom: 10px;
  color: #606266;
  line-height: 1.6;
}
.q-index {
  color: #409eff;
  font-weight: 600;
  margin-right: 6px;
}
.actions {
  margin-top: 16px;
  text-align: right;
}
.warn-text {
  color: #e6a23c;
  font-size: 12px;
  text-align: right;
  margin: 6px 0 0;
}
.result-date {
  color: #909399;
  font-size: 13px;
}
.self-harm {
  margin-bottom: 16px;
  line-height: 1.7;
}
.score-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 8px 0 16px;
}
.score-num {
  font-size: 36px;
  font-weight: bold;
  color: #303133;
}
.score-label {
  display: block;
  font-size: 12px;
  color: #909399;
}
.state-tag {
  font-size: 14px;
}
.dim-detail {
  margin-top: 8px;
}
.advice {
  margin-top: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 16px;
}
.advice-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
}
.advice ul {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.8;
}
.monitor-alert {
  margin-bottom: 12px;
  line-height: 1.7;
}
.empty-hint {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 12px 0;
}
.trend-chart {
  height: 240px;
  width: 100%;
}
.history-panel {
  margin-top: 0;
}
</style>
