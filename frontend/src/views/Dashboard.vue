<template>
  <div class="dashboard-container">
    <!-- 顶部：时间切换控制器 -->
    <div class="header">
      <h2>健康数据看板</h2>
      <el-radio-group v-model="timeRange" @change="fetchData">
        <el-radio-button label="7d">近 7 天</el-radio-button>
        <el-radio-button label="30d">近 30 天</el-radio-button>
        <el-radio-button label="all">全部历史</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 空状态 -->
    <el-empty
      v-if="!loading && trendList.length === 0"
      description="所选时间段暂无健康数据，请前往健康数据页面录入记录"
    />

    <!-- 数据展示区 -->
    <div v-else v-loading="loading">
      <!-- 1. 核心指标卡片区 -->
      <el-row :gutter="20" type="flex" class="card-row">
        <el-col :span="3" v-for="card in cardList" :key="card.label">
          <el-card shadow="hover" class="metric-card">
            <div class="card-icon">
              <!-- 心率：心电图脉搏线（EP 图标库无 heart 类图标，用自定义 SVG） -->
              <svg
                v-if="card.icon === 'heartbeat'"
                class="ecg-svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M2 12h4l2.5-6 3.5 12 2.5-7 1.5 3H22" />
              </svg>
              <el-icon v-else><component :is="card.icon" /></el-icon>
            </div>
            <div class="card-label">{{ card.label }}</div>
            <div class="card-value">
              {{ card.value }} <span class="unit">{{ card.unit }}</span>
            </div>
            <!-- 始终保留标签占位区，保证所有卡片等高；仅状态异常时才显示标签 -->
            <div class="card-tag-slot">
              <el-tag
                v-if="card.status && card.status !== '正常'"
                :type="card.statusType"
                size="small"
              >
                {{ card.status }}
              </el-tag>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 2. 统计小模块 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card shadow="never"
            >平均睡眠：<b>{{ stats.avgSleep }}</b> 小时</el-card
          >
        </el-col>
        <el-col :span="6">
          <el-card shadow="never"
            >平均心率：<b>{{ stats.avgHeart }}</b> 次/分</el-card
          >
        </el-col>
        <el-col :span="6">
          <el-card shadow="never"
            >体重波动：<b>{{ stats.minWeight }}</b> ~
            <b>{{ stats.maxWeight }}</b> kg</el-card
          >
        </el-col>
        <el-col :span="6">
          <el-card shadow="never"
            >步数达标（>8000）：<b>{{ stats.stepDays }}</b> 天</el-card
          >
        </el-col>
      </el-row>

      <!-- 3. 图表展示区 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <el-card shadow="hover"
            ><div ref="chart1Ref" class="chart-box"></div
          ></el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover"
            ><div ref="chart2Ref" class="chart-box"></div
          ></el-card>
        </el-col>
      </el-row>
      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <el-card shadow="hover"
            ><div ref="chart3Ref" class="chart-box"></div
          ></el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover"
            ><div ref="chart4Ref" class="chart-box"></div
          ></el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from "vue";
import * as echarts from "echarts";
import request from "@/utils/request";

const loading = ref(false);
const timeRange = ref("7d");
const trendList = ref([]);
const latestData = ref({});
const chart1Ref = ref(null);
const chart2Ref = ref(null);
const chart3Ref = ref(null);
const chart4Ref = ref(null);

let myChart1 = null,
  myChart2 = null,
  myChart3 = null,
  myChart4 = null;

// ========== 1. 获取数据 ==========
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.post("/health-data/dashboard", {
      timeRange: timeRange.value,
    });
    trendList.value = res.trendList || [];
    latestData.value = res.latestData || {};
    await nextTick();
    renderCharts();
  } finally {
    loading.value = false;
  }
};

// ========== 2. 计算顶部卡片数据 ==========
const cardList = computed(() => {
  const d = latestData.value;
  if (!d || !d.id) return [];

  // BMI 状态判定
  let bmiStatus = "正常",
    bmiType = "success";
  if (d.bmi < 18.5) {
    bmiStatus = "偏瘦";
    bmiType = "warning";
  } else if (d.bmi >= 24 && d.bmi < 28) {
    bmiStatus = "超重";
    bmiType = "warning";
  } else if (d.bmi >= 28) {
    bmiStatus = "肥胖";
    bmiType = "danger";
  }

  // 血压状态判定
  let bpStatus = "正常",
    bpType = "success";
  if (d.systolicPressure >= 140 || d.diastolicPressure >= 90) {
    bpStatus = "偏高";
    bpType = "danger";
  }

  return [
    { label: "身高", value: d.height, unit: "cm", icon: "TrendCharts" },
    { label: "体重", value: d.weight, unit: "kg", icon: "DataLine" },
    {
      label: "BMI",
      value: d.bmi,
      unit: "",
      icon: "Odometer",
      status: bmiStatus,
      statusType: bmiType,
    },
    {
      label: "血压",
      value: `${d.systolicPressure}/${d.diastolicPressure}`,
      unit: "mmHg",
      icon: "Aim",
      status: bpStatus,
      statusType: bpType,
    },
    { label: "心率", value: d.heartRate, unit: "次/分", icon: "heartbeat" },
    { label: "昨日步数", value: d.steps, unit: "步", icon: "Promotion" },
    { label: "昨日睡眠", value: d.sleepHours, unit: "小时", icon: "Moon" },
  ];
});

// ========== 3. 计算统计小模块 ==========
const stats = computed(() => {
  const list = trendList.value;
  if (!list.length)
    return {
      avgSleep: 0,
      avgHeart: 0,
      minWeight: 0,
      maxWeight: 0,
      stepDays: 0,
    };

  const sumSleep = list.reduce((acc, cur) => acc + (cur.sleepHours || 0), 0);
  const sumHeart = list.reduce((acc, cur) => acc + (cur.heartRate || 0), 0);
  const weights = list.map((item) => item.weight).filter((w) => w != null);
  const stepDays = list.filter((item) => item.steps > 8000).length;

  return {
    avgSleep: (sumSleep / list.length).toFixed(1),
    avgHeart: (sumHeart / list.length).toFixed(0),
    minWeight: weights.length ? Math.min(...weights).toFixed(1) : 0,
    maxWeight: weights.length ? Math.max(...weights).toFixed(1) : 0,
    stepDays,
  };
});

// ========== 4. 渲染 ECharts ==========
const renderCharts = () => {
  if (!trendList.value.length) return;

  const dates = trendList.value.map((item) => item.recordDate);
  const weights = trendList.value.map((item) => item.weight);
  const bmis = trendList.value.map((item) => item.bmi);
  const systolic = trendList.value.map((item) => item.systolicPressure);
  const diastolic = trendList.value.map((item) => item.diastolicPressure);

  // 心率异常高亮：>100 标红
  const heartRates = trendList.value.map((item) => {
    return {
      value: item.heartRate,
      itemStyle:
        item.heartRate > 100 ? { color: "#F56C6C" } : { color: "#409EFF" },
      symbolSize: item.heartRate > 100 ? 10 : 6,
    };
  });

  const steps = trendList.value.map((item) => item.steps);
  const sleeps = trendList.value.map((item) => item.sleepHours);

  // 通用配置
  const commonGrid = {
    left: "3%",
    right: "4%",
    bottom: "3%",
    containLabel: true,
  };
  const commonTooltip = { trigger: "axis" };

  // 图表①：体重 & BMI 双折线图
  myChart1 = echarts.init(chart1Ref.value);
  myChart1.setOption({
    backgroundColor: "#f8fafc",
    title: {
      text: "体重 & BMI 趋势",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: commonTooltip,
    legend: { data: ["体重(kg)", "BMI"], top: 5, left: 10 },
    grid: commonGrid,
    xAxis: { type: "category", data: dates },
    yAxis: [
      { type: "value", name: "体重(kg)" },
      { type: "value", name: "BMI", min: 15, max: 35 },
    ],
    series: [
      {
        name: "体重(kg)",
        type: "line",
        data: weights,
        smooth: true,
        itemStyle: { color: "#409EFF" },
      },
      {
        name: "BMI",
        type: "line",
        yAxisIndex: 1,
        data: bmis,
        smooth: true,
        itemStyle: { color: "#E6A23C" },
        markLine: {
          silent: true,
          data: [
            {
              yAxis: 18.5,
              lineStyle: { color: "#67C23A", type: "dashed" },
              label: { formatter: "偏瘦" },
            },
            {
              yAxis: 24,
              lineStyle: { color: "#E6A23C", type: "dashed" },
              label: { formatter: "超重" },
            },
            {
              yAxis: 28,
              lineStyle: { color: "#F56C6C", type: "dashed" },
              label: { formatter: "肥胖" },
            },
          ],
        },
      },
    ],
  });

  // 图表②：收缩压、舒张压双折线图
  myChart2 = echarts.init(chart2Ref.value);
  myChart2.setOption({
    backgroundColor: " #e2e9f0",
    title: {
      text: "血压趋势 (mmHg)",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: commonTooltip,
    legend: { data: ["收缩压", "舒张压"], top: 5, left: 10 },
    grid:{
      ...commonGrid,
      right:30,
    },
    xAxis: { type: "category", data: dates },
    yAxis: { type: "value" },
    series: [
      {
        name: "收缩压",
        type: "line",
        data: systolic,
        smooth: true,
        itemStyle: { color: "#F56C6C" },
        markLine: {
          data: [{ yAxis: 140, name: "收缩压偏高" }],
          lineStyle: { color: "#F56C6C", type: "dashed" },
        },
      },
      {
        name: "舒张压",
        type: "line",
        data: diastolic,
        smooth: true,
        itemStyle: { color: "#409EFF" },
        markLine: {
          data: [{ yAxis: 90, name: "舒张压偏高" }],
          lineStyle: { color: "#409EFF", type: "dashed" },
        },
      },
    ],
  });

  // 图表③：心率变化折线图
  myChart3 = echarts.init(chart3Ref.value);
  myChart3.setOption({
    backgroundColor: "#f7ebf2",
    title: {
      text: "心率变化 (次/分)",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: commonTooltip,
    grid:{
      ...commonGrid,
      right:50,
    },
    xAxis: { type: "category", data: dates },
    yAxis: { type: "value", min: 40, max: 140 },
    series: [
      {
        name: "心率",
        type: "line",
        data: heartRates,
        smooth: true,
        markLine: {
          silent: true,
          data: [
            {
              yAxis: 60,
              lineStyle: { color: "#909399", type: "dashed" },
              label: { formatter: "正常下限",textStyle:{fontSize:10} },
              
            },
            {
              yAxis: 100,
              lineStyle: { color: "#F56C6C", type: "dashed" },
              label: { formatter: "正常上限",textStyle:{fontSize:10} },
            },
          ],
        },
      },
    ],
  });

  // 图表④：步数 + 睡眠时长 组合柱状图
  myChart4 = echarts.init(chart4Ref.value);
  myChart4.setOption({
  backgroundColor: "#efebf0",
    title: {
      text: "步数 & 睡眠时长",
      left: "center",
      textStyle: { fontSize: 14 },
    },
    tooltip: commonTooltip,
    legend: { data: ["步数", "睡眠(小时)"], top: 5, left: 10 },
    grid: commonGrid,
    xAxis: { type: "category", data: dates },
    yAxis: [
      { type: "value", name: "步数" },
      { type: "value", name: "睡眠(h)", max: 24 },
    ],
    series: [
      {
        name: "步数",
        type: "bar",
        data: steps,
        itemStyle: { color: "#409EFF" },
        markLine: {
          data: [{ yAxis: 8000, name: "目标步数" }],
          lineStyle: { color: "#E6A23C", type: "dashed" },
        },
      },
      {
        name: "睡眠(小时)",
        type: "bar",
        yAxisIndex: 1,
        data: sleeps,
        itemStyle: { color: "#67C23A" },
        markLine: {
          silent: true,
          data: [
            { yAxis: 7, lineStyle: { color: "#909399", type: "dashed" } },
            { yAxis: 9, lineStyle: { color: "#909399", type: "dashed" } },
          ],
        },
      },
    ],
  });
};

// 窗口缩放适配
const handleResize = () => {
  myChart1?.resize();
  myChart2?.resize();
  myChart3?.resize();
  myChart4?.resize();
};

onMounted(() => {
  fetchData();
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  myChart1?.dispose();
  myChart2?.dispose();
  myChart3?.dispose();
  myChart4?.dispose();
});
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.card-row,
.stats-row,
.chart-row {
  margin-bottom: 20px;
}
.metric-card {
  text-align: center;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  cursor: pointer;
  height: 100%;                 /* 让同一行各卡等高 */
}
/* 悬浮：卡片上浮 + 放大 + 投影 */
.metric-card:hover {
  transform: translateY(-8px) scale(1.05);
  box-shadow: 0 12px 28px rgba(159, 29, 29, 0.18);
}
/* 标签占位区：始终占同一高度，没有状态标签的卡片也保留空间，保证整排等高 */
.card-tag-slot {
  min-height: 26px;
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 图标：圆形浅底 + 深红主色，悬浮时放大并加深 */
.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fbe9e9, #f5d5d5);
  color: #9f1d1d;
  line-height: 1;                 /* 统一基线，避免不同图标上下错位 */
  transition: transform 0.25s ease, color 0.25s ease, background 0.25s ease;

  .el-icon,
  .ecg-svg {
    width: 26px;
    height: 26px;
    font-size: 26px;              /* 让 EP 图标与 ECG 尺寸完全一致 */
  }

  .ecg-svg {
    stroke-width: 2;              /* 对齐 Element Plus 线型图标的描边粗细 */
  }
}
.metric-card:hover .card-icon {
  transform: scale(1.22);
  color: #7f1010;
  background: linear-gradient(135deg, #f8d7d7, #efbdbd);
}
.card-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}
.card-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  transition: color 0.25s ease;
}
.metric-card:hover .card-value {
  color: #7f1010;
}
.unit {
  font-size: 12px;
  font-weight: normal;
  color: #909399;
}
/* 统计小模块与图表卡片：统一悬浮上浮效果（不缩放，避免图表模糊） */
.stats-row .el-card,
.chart-row .el-card {
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}
.stats-row .el-card:hover,
.chart-row .el-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.1);
}
.chart-box {
  height: 350px;
  width: 100%;
}
</style>
