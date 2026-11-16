<template>
  <div class="health-plan-container">
    <!-- 顶部操作栏 -->
    <div class="header">
      <h2>健康计划管理</h2>
      <el-button type="primary" @click="openDialog()">+ 创建计划</el-button>
    </div>

    <!-- 置顶提醒栏 -->
    <div v-if="reminders.length" class="reminder-topbar" :class="{ collapsed: reminderCollapsed }">
      <div class="reminder-head">
        <span class="reminder-title">🔔 待做提醒（{{ reminders.length }}）</span>
        <div class="reminder-head-right">
          <span class="reminder-mail" :title="userEmail ? '提醒将发送至该邮箱' : '请先在个人中心填写 QQ 邮箱'">
            📧 {{ userEmail || '未设置邮箱' }}
          </span>
          <el-button link type="primary" size="small" :loading="testingMail" @click="sendTestMail">测试邮件</el-button>
          <el-button link type="info" size="small" @click="dismissAll">全部忽略</el-button>
        </div>
      </div>
      <div class="reminder-list" v-show="!reminderCollapsed">
        <div
          v-for="r in reminders"
          :key="r.id"
          class="reminder-item"
          :class="{ due: isDue(r) }"
        >
          <span class="reminder-dot"></span>
          <span class="reminder-name">{{ r.planName }}</span>
          <el-tag size="small" :type="getTypeTag(r.planType)">{{ r.planType }}</el-tag>
          <span class="reminder-time">
            {{ isDue(r) ? '⏰ 已到提醒时间 ' + r.remindTime : '🕒 将在 ' + (r.remindTime || '—') + ' 提醒' }}
          </span>
          <span class="reminder-hint">今天尚未打卡</span>
          <div class="reminder-actions">
            <el-button size="small" type="success" plain @click="goCheckin(r)">去打卡</el-button>
            <el-button size="small" type="danger" plain @click="dismiss(r.id)">删除</el-button>
          </div>
        </div>
      </div>
      <div class="reminder-collapse" @click="toggleReminder">
        <span>{{ reminderCollapsed ? '展开待做提醒' : '收起待做提醒' }}</span>
        <span class="arrow">{{ reminderCollapsed ? '▼' : '▲' }}</span>
      </div>
    </div>

    <!-- 表格区域 -->
    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="planName" label="计划名称" width="150" />
      <el-table-column prop="planType" label="计划类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.planType)">{{ row.planType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="startDate" label="开始日期" width="120" />
      <el-table-column prop="endDate" label="结束日期" width="120" />
      <el-table-column prop="remindTime" label="提醒时间" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="success">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="打卡" width="230">
        <template #default="{ row }">
          <div class="checkin-cell">
            <div class="checkin-dots">
              <span
                v-for="(on, i) in (checkinMap[row.id] && checkinMap[row.id].last7) || []"
                :key="i"
                :class="['dot', on ? 'on' : 'off']"
                :title="`${i === 6 ? '今天' : i - 6 + '天前'}`"
              ></span>
            </div>
            <div class="checkin-meta">
              <span class="checkin-text" v-if="checkinMap[row.id]">
                🔥 {{ checkinMap[row.id].streak }} 天 · 共
                {{ checkinMap[row.id].totalCount }} 次<span v-if="row.targetDays"> / 目标 {{ row.targetDays }} 天</span>
              </span>
              <div class="checkin-actions">
                <el-button
                  size="small"
                  :type="checkinMap[row.id] && checkinMap[row.id].checkedToday ? 'info' : 'success'"
                  :disabled="checkinMap[row.id] && checkinMap[row.id].checkedToday"
                  :loading="checkingId === row.id"
                  @click="handleCheckin(row)"
                >
                  {{ checkinMap[row.id] && checkinMap[row.id].checkedToday ? '已打卡' : '打卡' }}
                </el-button>
                <el-button size="small" type="primary" plain @click="openCalendar(row)">📅 日历</el-button>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openDialog(row)"
            >编辑</el-button
          >
          <el-button size="small" type="danger" @click="handleDelete(row.id)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="queryParams.pageNumber"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑计划' : '创建计划'"
      width="500px"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="计划名称" required>
          <el-input v-model="form.planName" placeholder="如：晨跑计划" />
        </el-form-item>
        <el-form-item label="计划类型" required>
          <el-select v-model="form.planType" placeholder="请选择">
            <el-option label="运动" value="运动" />
            <el-option label="饮食" value="饮食" />
            <el-option label="睡眠" value="睡眠" />
            <el-option label="综合" value="综合" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker
            v-model="form.endDate"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="提醒时间" required>
          <el-time-picker
            v-model="form.remindTime"
            value-format="HH:mm:ss"
            placeholder="选择时间"
          />
        </el-form-item>
        <el-form-item label="目标打卡天数">
          <el-input-number v-model="form.targetDays" :min="0" :controls="false" placeholder="留空=按计划天数" />
          <span class="form-tip">累计打卡达到该天数即自动标记为「已完成」（0 或留空则按计划结束日期判定）</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已暂停" value="已暂停" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
        </template>
      </el-dialog>

      <!-- 打卡日历大图 -->
      <el-dialog v-model="calendarVisible" :title="`📅 打卡日历 · ${calendarPlanName}`" width="520px">
        <div class="cal-summary">
          累计打卡 <b>{{ calendarDates.size }}</b> 天
        </div>
        <el-calendar v-model="calendarDate">
          <template #date-cell="{ data }">
            <div class="cal-cell" :class="{ checked: calendarDates.has(data.day) }">
              <span class="cal-day">{{ data.day.split('-')[2] }}</span>
              <span v-if="calendarDates.has(data.day)" class="cal-dot"></span>
            </div>
          </template>
        </el-calendar>
      </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox, ElNotification } from "element-plus";
import { getPlanList, addPlan, updatePlan, deletePlan, checkinPlan, getCheckinOverview, fetchCheckinDates, sendTestReminder } from "@/api/healthPlan";

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
// 打卡统计：planId -> { totalCount, streak, weekCount, checkedToday, last7 }
const checkinMap = ref({});
const checkingId = ref(null);
// 打卡日历
const calendarVisible = ref(false);
const calendarPlanId = ref(null);
const calendarPlanName = ref("");
const calendarDates = ref(new Set());
const calendarDate = ref(new Date());

// 获取当前登录用户信息
const userInfo = JSON.parse(localStorage.getItem("userInfo") || "{}");

// 查询参数
const queryParams = reactive({
  pageNumber: 1,
  pageSize: 10,
  planName: "",
  planType: "",
  status: "",
});

// 表单数据
const form = reactive({
  id: null,
  planName: "",
  planType: "",
  description: "",
  startDate: "",
  endDate: "",
  remindTime: "",
  targetDays: null,
  status: "进行中",
});

// ============ 置顶提醒功能 ============
// 当前时间（每分钟刷新，用于判断是否到达每日提醒时间）
const now = ref(new Date());
// 提醒栏是否收起（折叠状态持久化，避免长列表遮挡下方计划表格）
const reminderCollapsed = ref(false);
try {
  if (localStorage.getItem("health_reminder_collapsed") === "1") reminderCollapsed.value = true;
} catch (e) {}
const toggleReminder = () => {
  reminderCollapsed.value = !reminderCollapsed.value;
  try {
    localStorage.setItem("health_reminder_collapsed", reminderCollapsed.value ? "1" : "0");
  } catch (e) {}
};
// 当天已忽略（删除）的提醒计划 id，存 localStorage 当天有效
const dismissedIds = ref([]);
// 当天已弹过通知的计划 id，避免重复通知
const notifiedIds = ref([]);

const todayStr = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

// 加载当天已忽略的提醒
const loadDismissed = () => {
  try {
    const raw = localStorage.getItem(`health_reminder_dismissed_${todayStr()}`);
    dismissedIds.value = raw ? JSON.parse(raw) : [];
  } catch (e) {
    dismissedIds.value = [];
  }
};
const persistDismissed = () => {
  localStorage.setItem(`health_reminder_dismissed_${todayStr()}`, JSON.stringify(dismissedIds.value));
};

// 判断某计划是否已到达当日提醒时间（真正运用 remindTime）
const isDue = (plan) => {
  if (!plan.remindTime) return true; // 未设时间则视为始终提醒
  const [h, m, s] = plan.remindTime.split(":").map(Number);
  const due = new Date();
  due.setHours(h, m || 0, s || 0, 0);
  return now.value >= due;
};

// 待做提醒：进行中 + 今天尚未打卡 + 未被忽略
const reminders = computed(() =>
  tableData.value.filter((p) => {
    if (p.status !== "进行中") return false;
    const ov = checkinMap.value[p.id];
    if (ov && ov.checkedToday) return false;
    if (dismissedIds.value.includes(p.id)) return false;
    return true;
  })
);

// 从待做提醒中删除（忽略当天）
const dismiss = (id) => {
  if (!dismissedIds.value.includes(id)) {
    dismissedIds.value = [...dismissedIds.value, id];
    persistDismissed();
  }
};
const dismissAll = () => {
  const ids = reminders.value.map((r) => r.id);
  dismissedIds.value = [...new Set([...dismissedIds.value, ...ids])];
  persistDismissed();
};

// 去打卡：点击后直接打卡，打卡成功自动移出提醒
const goCheckin = async (row) => {
  await handleCheckin(row);
};

// 发送测试提醒邮件（验证 SMTP 配置是否生效）
const userEmail = ref(userInfo.email || "");
const testingMail = ref(false);
const sendTestMail = async () => {
  testingMail.value = true;
  try {
    const res = await sendTestReminder();
    ElMessage.success(typeof res === "string" ? res : "测试邮件已发送，请查收");
  } catch (e) {
    ElMessage.error(e?.message || "发送失败");
  } finally {
    testingMail.value = false;
  }
};

// 每分钟检查：刷新当前时间，并对到达提醒时间且未忽略的计划弹通知
let reminderTimer = null;
const checkDueNotifications = () => {
  reminders.value.forEach((p) => {
    if (isDue(p) && !notifiedIds.value.includes(p.id)) {
      notifiedIds.value = [...notifiedIds.value, p.id];
      ElNotification({
        title: "🔔 健康提醒",
        message: `「${p.planName}」的提醒时间已到，记得完成今日${p.planType}打卡~`,
        type: "warning",
        duration: 4500,
      });
    }
  });
};

// 获取数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getPlanList(queryParams);
    tableData.value = res.list;
    total.value = res.total;
    await loadCheckinOverview();
  } finally {
    loading.value = false;
  }
};

// 加载所有计划的打卡概览，挂到 checkinMap 上
const loadCheckinOverview = async () => {
  if (!tableData.value.length) {
    checkinMap.value = {};
    return;
  }
  try {
    const res = await getCheckinOverview(tableData.value.map((d) => d.id));
    checkinMap.value = res || {};
  } catch (e) {
    console.error("加载打卡概览失败:", e);
  }
};

// 打卡
const handleCheckin = async (row) => {
  checkingId.value = row.id;
  try {
    await checkinPlan(row.id);
    ElMessage.success("打卡成功，继续坚持 💪");
    await loadCheckinOverview();
  } catch (e) {
    console.error("打卡失败:", e);
  } finally {
    checkingId.value = null;
  }
};

// 打开打卡日历
const openCalendar = async (row) => {
  calendarPlanId.value = row.id;
  calendarPlanName.value = row.planName;
  calendarVisible.value = true;
  try {
    const res = await fetchCheckinDates(row.id);
    calendarDates.value = new Set(res || []);
  } catch (e) {
    calendarDates.value = new Set();
  }
};

// 打开弹窗
const openDialog = (row = null) => {
  if (row) {
    Object.assign(form, row); // 编辑：回显数据
  } else {
    // 新增：重置表单
    Object.assign(form, {
      id: null,
      planName: "",
      planType: "",
      description: "",
      startDate: "",
      endDate: "",
      remindTime: "",
      targetDays: null,
      status: "进行中",
    });
  }
  dialogVisible.value = true;
};

// 提交表单
const submitForm = async () => {
  if (
    !form.planName ||
    !form.planType ||
    !form.startDate ||
    !form.endDate ||
    !form.remindTime
  ) {
    return ElMessage.warning("请填写完整必填项");
  }
  // 目标打卡天数：留空则按计划起止天数自动计算
  if (form.targetDays === "" || form.targetDays == null) {
    if (form.startDate && form.endDate) {
      const s = new Date(form.startDate);
      const e = new Date(form.endDate);
      const days = Math.round((e - s) / 86400000) + 1;
      form.targetDays = days > 0 ? days : 0;
    } else {
      form.targetDays = 0;
    }
  }
  if (form.id) {
    await updatePlan(form);
    ElMessage.success("更新成功");
  } else {
    await addPlan(form);
    ElMessage.success("创建成功");
  }
  dialogVisible.value = false;
  fetchData();
};

// 删除
const handleDelete = (id) => {
  ElMessageBox.confirm("确定删除该计划吗？", "提示", { type: "warning" }).then(
    async () => {
      await deletePlan(id);
      ElMessage.success("删除成功");
      fetchData();
    },
  );
};

// 类型标签颜色
const getTypeTag = (type) => {
  const map = { 运动: "success", 饮食: "warning", 睡眠: "info", 综合: "primary" };
  return map[type] || "primary";
};

onMounted(() => {
  now.value = new Date();
  loadDismissed();
  reminderTimer = setInterval(() => {
    now.value = new Date();
    checkDueNotifications();
  }, 60 * 1000);
  fetchData();
});

onUnmounted(() => {
  if (reminderTimer) clearInterval(reminderTimer);
});
</script>

<style scoped>
.health-plan-container {
  padding: 20px;
  background: #fff;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
/* 打卡单元格 */
.checkin-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.checkin-dots {
  display: flex;
  gap: 4px;
}
.checkin-dots .dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  transition: background 0.2s ease;
}
.checkin-dots .dot.on {
  background: #9f1d1d; /* 已打卡：深红 */
  box-shadow: 0 0 4px rgba(159, 29, 29, 0.4);
}
.checkin-dots .dot.off {
  background: #ededed; /* 未打卡：浅灰 */
}
.checkin-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.checkin-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
/* 去掉 EP 相邻按钮默认 margin，间距统一由 gap 控制，保证两按钮等高对齐 */
.checkin-actions .el-button + .el-button {
  margin-left: 0;
}
.checkin-text {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
/* 创建/编辑表单提示 */
.form-tip {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}
/* 打卡日历 */
.cal-summary {
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}
.cal-summary b {
  color: #9f1d1d;
  font-size: 16px;
}
.cal-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.cal-cell.checked {
  background: #fdeaea;
  border-radius: 6px;
}
.cal-cell .cal-day {
  font-size: 13px;
}
.cal-cell .cal-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9f1d1d;
  margin-top: 2px;
}
/* 置顶提醒栏 */
.reminder-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: linear-gradient(135deg, #fff7e6 0%, #fff1f0 100%);
  border: 1px solid #ffd591;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.reminder-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.reminder-title {
  font-size: 15px;
  font-weight: 600;
  color: #d4380d;
}
.reminder-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.reminder-mail {
  font-size: 13px;
  color: #595959;
}
.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.reminder-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #ffe7ba;
  border-radius: 8px;
  transition: all 0.2s ease;
}
.reminder-item.due {
  border-color: #ffa39e;
  background: #fff1f0;
}
.reminder-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #faad14;
  flex-shrink: 0;
}
.reminder-item.due .reminder-dot {
  background: #f5222d;
  box-shadow: 0 0 6px rgba(245, 34, 45, 0.6);
}
.reminder-name {
  font-weight: 600;
  color: #262626;
}
.reminder-time {
  font-size: 13px;
  color: #fa8c16;
}
.reminder-item.due .reminder-time {
  color: #cf1322;
  font-weight: 600;
}
.reminder-hint {
  font-size: 12px;
  color: #8c8c8c;
}
.reminder-actions {
  margin-left: auto;
}
/* 收起/展开箭头条 */
.reminder-collapse {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 6px;
  padding: 6px 0 2px;
  cursor: pointer;
  color: #d4380d;
  font-size: 13px;
  user-select: none;
  border-top: 1px dashed #ffd591;
  transition: background 0.2s ease;
}
.reminder-collapse:hover {
  background: rgba(255, 255, 255, 0.55);
}
.reminder-collapse .arrow {
  font-size: 12px;
  line-height: 1;
}
/* 折叠态：缩小内边距，仅保留标题与箭头条 */
.reminder-topbar.collapsed {
  padding-bottom: 6px;
}
</style>
