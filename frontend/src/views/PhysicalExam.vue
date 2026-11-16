<template>
  <div class="pe-container">
    <!-- 顶部：报告选择 / 新建 -->
    <el-card shadow="never" class="pe-card">
      <div class="pe-head">
        <div class="head-left">
          <h2>体检报告</h2>
          <span class="sub-tip">录入体检指标，自动标记异常，并交由 AI 通俗解读</span>
        </div>
        <el-button type="primary" size="small" :icon="Plus" @click="createNew">+ 新建报告</el-button>
      </div>

      <div v-if="reports.length" class="report-switch">
        <span class="switch-label">历史报告：</span>
        <el-select v-model="currentId" placeholder="选择报告" style="width: 240px" @change="selectReport">
          <el-option
            v-for="r in reports"
            :key="r.id"
            :label="`${r.examDate} ${r.hospital ? '· ' + r.hospital : ''}`"
            :value="r.id"
          />
        </el-select>
        <el-button type="danger" plain size="small" @click="handleDelete">删除本报告</el-button>
      </div>
      <el-empty v-else description="还没有体检报告，点击右上角「新建报告」开始录入" :image-size="70" />
    </el-card>

    <!-- 录入 / 详情表格 -->
    <el-card v-if="isEditing" shadow="never" class="pe-card">
      <div class="form-row">
        <div class="form-item">
          <label>体检日期</label>
          <el-date-picker
            v-model="form.examDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 200px"
          />
        </div>
        <div class="form-item">
          <label>体检机构</label>
          <el-input v-model="form.hospital" placeholder="如：市第一人民医院（选填）" style="width: 280px" />
        </div>
      </div>

      <div class="table-hint">
        <el-tag type="success" size="small" effect="dark">绿 · 正常</el-tag>
        <el-tag type="danger" size="small" effect="dark">红 · 异常</el-tag>
        <el-tag type="info" size="small" effect="dark">灰 · 未录入</el-tag>
        <span class="hint-tip">填入数值后自动判定，参考范围因医院与性别略有差异</span>
      </div>

      <div v-for="cat in catalog.categories" :key="cat.key" class="cat-block">
        <div class="cat-title">
          <span class="cat-bar"></span>{{ cat.label }}
          <span class="cat-count">{{ itemsByCat(cat.key).length }} 项</span>
        </div>
        <el-table :data="itemsByCat(cat.key)" border stripe size="small" class="exam-table">
          <el-table-column label="指标名称" min-width="170">
            <template #default="{ row }">
              <span class="item-name">{{ row.name }}</span>
              <el-tooltip :content="row.tip" placement="top" v-if="row.tip">
                <el-icon class="tip-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="参考范围" width="150" align="center">
            <template #default="{ row }"><span class="ref-text">{{ row.refText }}</span></template>
          </el-table-column>
          <el-table-column label="单位" width="100" align="center">
            <template #default="{ row }"><span class="unit-text">{{ row.unit || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="录入值" width="170" align="center">
            <template #default="{ row }">
              <el-input
                v-model="form.values[row.key]"
                type="number"
                size="small"
                placeholder="未录入"
                @input="onValueInput(row.key)"
              />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="statusOf(row) === 'normal'" type="success" size="small">正常</el-tag>
              <el-tag v-else-if="statusOf(row) === 'high'" type="danger" size="small">偏高 ↑</el-tag>
              <el-tag v-else-if="statusOf(row) === 'low'" type="danger" size="small">偏低 ↓</el-tag>
              <el-tag v-else type="info" size="small">未录入</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="save-row">
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveReport">保存报告</el-button>
      </div>
    </el-card>

    <!-- 详情只读模式：展示已保存表格 -->
    <el-card v-else-if="detailView" shadow="never" class="pe-card">
      <div class="detail-meta">
        <span><b>体检日期：</b>{{ detailView.exam.examDate }}</span>
        <span><b>体检机构：</b>{{ detailView.exam.hospital || '—' }}</span>
        <el-button link type="primary" size="small" @click="editCurrent">编辑此报告</el-button>
      </div>
      <div class="table-hint">
        <el-tag type="success" size="small" effect="dark">绿 · 正常</el-tag>
        <el-tag type="danger" size="small" effect="dark">红 · 异常</el-tag>
      </div>
      <div v-for="cat in catalog.categories" :key="cat.key" class="cat-block">
        <div class="cat-title"><span class="cat-bar"></span>{{ cat.label }}</div>
        <el-table :data="detailRowsByCat(cat.key)" border stripe size="small" class="exam-table">
          <el-table-column label="指标名称" min-width="170" prop="name" />
          <el-table-column label="参考范围" width="150" align="center" prop="refText" />
          <el-table-column label="单位" width="100" align="center">
            <template #default="{ row }">{{ row.unit || '—' }}</template>
          </el-table-column>
          <el-table-column label="结果" width="130" align="center">
            <template #default="{ row }">
              <b :class="valueClass(row.status)">{{ row.value != null ? row.value : '—' }}</b>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'normal'" type="success" size="small">正常</el-tag>
              <el-tag v-else-if="row.status === 'high'" type="danger" size="small">偏高 ↑</el-tag>
              <el-tag v-else-if="row.status === 'low'" type="danger" size="small">偏低 ↓</el-tag>
              <el-tag v-else type="info" size="small">未录入</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- AI 健康解读（表格下方独立区域） -->
    <el-card v-if="currentId && !isEditing" shadow="never" class="pe-card ai-panel">
      <div class="pe-head">
        <div class="head-left">
          <h2>AI 健康解读</h2>
          <span class="sub-tip">将全部异常项批量交给 AI，用大白话说明风险、给出建议与复查提醒</span>
        </div>
        <el-button type="primary" size="small" :loading="interpreting" :icon="MagicStick" @click="doInterpret">
          {{ interpretation ? '重新解读' : '生成解读' }}
        </el-button>
      </div>

      <div v-if="interpretLoading" class="ai-loading">
        <el-skeleton :rows="6" animated />
        <p>AI 正在分析你的体检指标，请稍候…</p>
      </div>

      <div v-else-if="interpretation" class="ai-result">
        <div v-for="(sec, i) in interpretSections" :key="i" class="ai-section">
          <div class="ai-section-title">
            <el-icon><component :is="sectionIcon(i)" /></el-icon>{{ sec.title }}
          </div>
          <div class="ai-section-body">
            <div v-for="(line, li) in sec.lines" :key="li" class="ai-line">{{ line }}</div>
          </div>
        </div>
        <div class="ai-disclaimer">以上解读由 AI 生成，仅供参考，具体请咨询专业医生。</div>
      </div>

      <el-empty v-else description="还没有解读，点击右上角「生成解读」" :image-size="70" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MagicStick, QuestionFilled, TrendCharts, Apple, AlarmClock } from '@element-plus/icons-vue'
import {
  getExamCatalog, getExamList, getExamDetail,
  saveExam, deleteExam, interpretExam,
} from '@/api/physicalExam'

const catalog = ref({ categories: [], items: [] })
const reports = ref([])
const currentId = ref(null)
const saving = ref(false)
const isEditing = ref(false)
const detailView = ref(null)

const form = reactive({ examDate: '', hospital: '', values: {} })
const interpretation = ref('')
const interpreting = ref(false)
const interpretLoading = ref(false)

const sectionIcons = [TrendCharts, Apple, AlarmClock]
const sectionIcon = (i) => sectionIcons[i] || TrendCharts

onMounted(async () => {
  try {
    catalog.value = await getExamCatalog()
  } catch (e) { /* 拦截器已提示 */ }
  await loadReports()
})

async function loadReports() {
  try {
    reports.value = await getExamList()
    if (reports.value.length) {
      await selectReport(reports.value[0].id)
    } else {
      currentId.value = null
      detailView.value = null
    }
  } catch (e) {}
}

async function selectReport(id) {
  if (!id) return
  currentId.value = id          // 关键：设置当前报告 id，否则下方【AI健康解读】区会被隐藏
  isEditing.value = false
  try {
    detailView.value = await getExamDetail(id)
    interpretation.value = detailView.value.interpretation || ''
  } catch (e) {}
}

function itemsByCat(catKey) {
  return catalog.value.items.filter((i) => i.category === catKey)
}
function detailRowsByCat(catKey) {
  return (detailView.value?.details || []).filter((i) => i.category === catKey)
}

// 根据参考范围实时判定录入值状态
function statusOf(item) {
  const raw = form.values[item.key]
  if (raw === '' || raw === null || raw === undefined) return 'none'
  const v = Number(raw)
  if (Number.isNaN(v)) return 'none'
  if (item.refLow == null && item.refHigh == null) return 'none'
  if (item.refLow != null && v < Number(item.refLow)) return 'low'
  if (item.refHigh != null && v > Number(item.refHigh)) return 'high'
  return 'normal'
}

function onValueInput(key) {
  // 数字输入保持字符串，保存时统一转成数字
}

function createNew() {
  currentId.value = null
  detailView.value = null
  interpretation.value = ''
  form.examDate = new Date().toISOString().slice(0, 10)
  form.hospital = ''
  form.values = {}
  isEditing.value = true
}

function editCurrent() {
  if (!detailView.value) return
  form.examDate = detailView.value.exam.examDate
  form.hospital = detailView.value.exam.hospital || ''
  const values = {}
  ;(detailView.value.details || []).forEach((d) => {
    if (d.value != null) values[d.key] = d.value
  })
  form.values = values
  isEditing.value = true
}

function cancelEdit() {
  isEditing.value = false
  if (currentId.value) selectReport(currentId.value)
  else if (reports.value.length) {
    currentId.value = reports.value[0].id
    selectReport(currentId.value)
  }
}

async function saveReport() {
  if (!form.examDate) return ElMessage.warning('请选择体检日期')
  const items = {}
  let count = 0
  Object.keys(form.values).forEach((k) => {
    const raw = form.values[k]
    if (raw === '' || raw === null || raw === undefined) return
    const num = Number(raw)
    if (!Number.isNaN(num)) { items[k] = num; count++ }
  })
  if (!count) return ElMessage.warning('请至少录入一项指标')

  saving.value = true
  try {
    const payload = {
      id: currentId.value || undefined,
      examDate: form.examDate,
      hospital: form.hospital,
      items,
    }
    const newId = await saveExam(payload)
    ElMessage.success('保存成功')
    isEditing.value = false
    await loadReports()
    if (newId) {
      currentId.value = newId
      await selectReport(newId)
    }
  } catch (e) {} finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!currentId.value) return
  try {
    await ElMessageBox.confirm('确定删除这份体检报告吗？', '提示', { type: 'warning' })
    await deleteExam(currentId.value)
    ElMessage.success('已删除')
    await loadReports()
  } catch (e) {}
}

async function doInterpret() {
  if (!currentId.value) return
  interpretLoading.value = true
  interpreting.value = true
  try {
    interpretation.value = await interpretExam(currentId.value)
  } catch (e) {} finally {
    interpretLoading.value = false
    interpreting.value = false
  }
}

// 把 AI 文本按【标题】拆成若干段
const interpretSections = computed(() => {
  if (!interpretation.value) return []
  const text = interpretation.value.replace(/\r/g, '')
  const parts = text.split(/【([^】]+)】/).filter((s) => s.trim())
  const sections = []
  for (let i = 0; i < parts.length; i += 2) {
    const title = parts[i]
    let body = parts[i + 1] || ''
    const lines = body.split('\n').map((l) => l.trim()).filter(Boolean)
    sections.push({ title, lines })
  }
  if (!sections.length) sections.push({ title: '解读', lines: text.split('\n').filter(Boolean) })
  return sections
})

function valueClass(status) {
  if (status === 'high' || status === 'low') return 'val-abnormal'
  if (status === 'normal') return 'val-normal'
  return ''
}
</script>

<style scoped>
.pe-container { padding: 4px; }
.pe-card { margin-bottom: 18px; }
.pe-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.head-left { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; }
.head-left h2 { margin: 0; font-size: 18px; color: #303133; }
.sub-tip { font-size: 12px; color: #909399; }
.report-switch { display: flex; align-items: center; gap: 10px; margin-top: 6px; }
.switch-label { font-size: 13px; color: #606266; }

.form-row { display: flex; gap: 24px; margin-bottom: 16px; flex-wrap: wrap; }
.form-item { display: flex; align-items: center; gap: 10px; }
.form-item label { font-size: 13px; color: #606266; white-space: nowrap; }

.table-hint { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; flex-wrap: wrap; }
.hint-tip { font-size: 12px; color: #c0c4cc; margin-left: 6px; }

.cat-block { margin-bottom: 22px; }
.cat-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #303133; margin-bottom: 10px; }
.cat-bar { width: 4px; height: 16px; border-radius: 2px; background: #9f1d1d; }
.cat-count { font-size: 12px; font-weight: normal; color: #909399; }
.item-name { color: #303133; font-size: 13px; }
.tip-icon { color: #c0c4cc; margin-left: 4px; cursor: help; vertical-align: middle; }
.ref-text { color: #606266; font-size: 12px; }
.unit-text { color: #909399; font-size: 12px; }
.exam-table { font-size: 13px; }

.save-row { text-align: right; margin-top: 6px; }
.detail-meta { display: flex; align-items: center; gap: 24px; margin-bottom: 14px; font-size: 13px; color: #606266; flex-wrap: wrap; }
.detail-meta b { color: #303133; }
.val-normal { color: #18a058; }
.val-abnormal { color: #d03050; }

.ai-panel { border-left: 3px solid #9f1d1d; }
.ai-loading p { text-align: center; color: #909399; font-size: 13px; margin-top: 12px; }
.ai-result { display: flex; flex-direction: column; gap: 16px; }
.ai-section { background: #fafafa; border-radius: 8px; padding: 14px 16px; }
.ai-section-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; color: #9f1d1d; margin-bottom: 8px; }
.ai-section-body { padding-left: 4px; }
.ai-line { font-size: 13px; color: #50565e; line-height: 1.8; padding-left: 14px; position: relative; }
.ai-line::before { content: '•'; position: absolute; left: 2px; color: #9f1d1d; }
.ai-disclaimer { font-size: 12px; color: #c0c4cc; text-align: right; }
</style>
