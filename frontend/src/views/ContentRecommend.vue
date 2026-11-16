<template>
  <div class="cr-container">
    <!-- 顶部说明 -->
    <el-card shadow="never" class="intro">
      <div class="intro-head">
        <h2>内容推荐</h2>
        <el-button :loading="loading" @click="load" size="small" :icon="Refresh">
          重新生成
        </el-button>
      </div>
      <p class="intro-text">
        根据你的<strong>健康数据</strong>与<strong>心理健康测评</strong>，为你个性化推荐视频、音乐与科普内容，
        帮助你针对性地改善体重、睡眠、运动与情绪状态。
      </p>
    </el-card>

    <el-row v-loading="loading">
      <!-- 无数据空状态 -->
      <el-col :span="24" v-if="!loading && !hasAnyData">
        <el-empty description="暂无可用于推荐的数据">
          <template #description>
            <div class="empty-desc">
              先补充<strong>健康数据</strong>或完成一次<strong>心理健康测评</strong>，<br />
              我们就能为你生成专属内容推荐。
            </div>
          </template>
          <div class="empty-actions">
            <el-button type="primary" @click="goHealth">去填写健康数据</el-button>
            <el-button @click="goMental">去做心理测评</el-button>
          </div>
        </el-empty>
      </el-col>

      <!-- 需求画像 -->
      <el-col :span="24" v-if="hasAnyData">
        <div class="profile">
          <span class="profile-label">当前关注：</span>
          <el-tag
            v-for="n in result.needs"
            :key="n.key"
            :color="n.color"
            effect="dark"
            class="need-tag"
          >
            <el-icon><component :is="n.icon" /></el-icon>{{ n.label }}
          </el-tag>
        </div>
      </el-col>

      <!-- 分组推荐：整张卡片点击跳转 -->
      <el-col :span="24" v-for="g in result.groups" :key="g.key" class="group">
        <div class="group-head">
          <span class="group-icon" :style="{ background: g.color }">
            <el-icon><component :is="g.icon" /></el-icon>
          </span>
          <div class="group-title">
            <span class="g-name">{{ g.label }}</span>
            <span class="g-reason">{{ g.reason }}</span>
          </div>
        </div>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="8" v-for="item in g.items" :key="item.id" class="card-col">
            <a
              class="rec-card-link"
              :href="item.url"
              target="_blank"
              rel="noopener noreferrer"
            >
              <el-card shadow="hover" class="rec-card">
                <div class="media-thumb" :class="item.type">
                  <el-icon class="thumb-icon"><component :is="typeIcon(item.type)" /></el-icon>
                  <span class="thumb-badge">{{ typeLabel(item.type) }}</span>
                </div>
                <div class="card-body">
                  <div class="card-title">{{ item.title }}</div>
                  <div class="card-desc">{{ item.desc || item.excerpt }}</div>
                  <span class="card-cta">前往查看 ↗</span>
                </div>
              </el-card>
            </a>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getHistory, analyzeTrend } from '@/utils/mentalHealth'
import { buildRecommendations } from '@/utils/recommendEngine'

const router = useRouter()
const loading = ref(false)
const result = ref({ needs: [], groups: [], total: 0 })

const hasAnyData = computed(() => result.value.needs.length > 0)

// 内容类型 → 图标 / 文案（图标名取 Element Plus 已注册图标）
const TYPE_MAP = {
  video: { icon: 'VideoCamera', label: '视频' },
  music: { icon: 'Headset', label: '音乐' },
  article: { icon: 'Document', label: '图文' },
}
const typeIcon = (t) => TYPE_MAP[t]?.icon || 'Document'
const typeLabel = (t) => TYPE_MAP[t]?.label || '内容'

// 由健康数据列表计算摘要（取最新一条 + 近 7 天均值）
function buildHealthSummary(list) {
  if (!list || !list.length) return { hasData: false }
  const sorted = [...list].sort(
    (a, b) => new Date(b.recordDate) - new Date(a.recordDate),
  )
  const latest = sorted[0]
  const now = Date.now()
  const last7 = sorted.filter(
    (r) => now - new Date(r.recordDate).getTime() <= 7 * 86400000,
  )
  const pool = last7.length ? last7 : sorted
  const avg = (k) => {
    const xs = pool.map((r) => r[k]).filter((v) => v != null)
    return xs.length ? xs.reduce((s, v) => s + v, 0) / xs.length : null
  }
  return {
    hasData: true,
    bmi: latest.bmi,
    sleepAvg: avg('sleepHours'),
    stepsAvg: avg('steps'),
    systolic: latest.systolicPressure,
    diastolic: latest.diastolicPressure,
    heartRate: latest.heartRate,
  }
}

// 由本地心理测评记录计算摘要
function buildMentalSummary() {
  const history = getHistory()
  if (!history || !history.length) return { hasData: false }
  const latest = [...history].sort(
    (a, b) => new Date(b.date) - new Date(a.date),
  )[0]
  return {
    hasData: true,
    latest: {
      wellness: latest.wellness,
      dep: latest.dep,
      anx: latest.anx,
      sleep: latest.sleep,
      stress: latest.stress,
    },
    trend: analyzeTrend(history),
  }
}

async function load() {
  loading.value = true
  try {
    // 健康数据（需登录；失败则视为无数据，不阻断心理侧推荐）
    let health = { hasData: false }
    try {
      const res = await request.post('/health-data/list', {
        pageNumber: 1,
        pageSize: 30,
      })
      health = buildHealthSummary(res?.list || [])
    } catch (e) {
      health = { hasData: false }
    }

    const mental = buildMentalSummary()
    result.value = buildRecommendations({ health, mental })
  } finally {
    loading.value = false
  }
}

const goHealth = () => router.push('/back/knowledge')
const goMental = () => router.push('/back/mental-health')

onMounted(load)
</script>

<style scoped>
.cr-container {
  padding: 4px;
}
.intro {
  margin-bottom: 16px;
}
.intro-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.intro-head h2 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}
.intro-text {
  color: #606266;
  line-height: 1.7;
  margin: 0;
  font-size: 14px;
}
.empty-desc {
  line-height: 1.9;
  color: #606266;
}
.empty-actions {
  margin-top: 8px;
}
.profile {
  margin: 4px 0 20px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.profile-label {
  color: #909399;
  font-size: 13px;
}
.need-tag {
  border: none;
  color: #fff;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.group {
  margin-bottom: 24px;
}
.group-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.group-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}
.group-title {
  display: flex;
  flex-direction: column;
}
.g-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.g-reason {
  font-size: 12px;
  color: #909399;
}
.card-col {
  margin-bottom: 16px;
}
.rec-card-link {
  display: block;
  text-decoration: none;
  color: inherit;
}
.rec-card {
  height: 100%;
  transition: transform 0.15s ease;
}
.rec-card:hover {
  transform: translateY(-3px);
}
.media-thumb {
  position: relative;
  height: 120px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}
.media-thumb.video {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}
.media-thumb.music {
  background: linear-gradient(135deg, #67c23a, #95d475);
}
.media-thumb.article {
  background: linear-gradient(135deg, #e6a23c, #f0c78a);
}
.thumb-icon {
  font-size: 42px;
  color: rgba(255, 255, 255, 0.95);
}
.thumb-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}
.card-body {
  padding: 0 4px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.4;
}
.card-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
  margin-bottom: 10px;
  min-height: 38px;
}
.card-cta {
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}
</style>
