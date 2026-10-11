<template>
  <div class="health-plan-container">
    <!-- 顶部操作栏 -->
    <div class="header">
      <h2>健康计划管理</h2>
      <el-button type="primary" @click="openDialog()">+ 创建计划</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { getPlanList, addPlan, updatePlan, deletePlan } from "@/api/healthPlan";

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const dialogVisible = ref(false);

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
  status: "进行中",
});

// 获取数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getPlanList(queryParams);
    tableData.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
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
  const map = { 运动: "success", 饮食: "warning", 睡眠: "info" };
  return map[type] || "primary";
};

onMounted(() => {
  fetchData();
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
</style>
