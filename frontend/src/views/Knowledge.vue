<template>
  <div class="health-data-container">
    <!-- 顶部：查询与操作栏 -->
    <div class="header">
      <h2>健康数据管理</h2>
      <div class="filter-box">
        <span>开始日期</span>
        <el-date-picker
          v-model="queryParams.startDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
        />
        <span>结束日期</span>
        <el-date-picker
          v-model="queryParams.endDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
        />
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button
          type="primary"
          style="margin-left: 15px"
          @click="openDialog()"
          >+ 添加数据</el-button
        >
      </div>
    </div>

    <!-- 表格区域 -->
    <el-table :data="tableData" style="width: 100%" v-loading="loading">
      <el-table-column prop="recordDate" label="记录日期" width="120" />
      <el-table-column prop="weight" label="体重(kg)" width="100" />
      <el-table-column prop="height" label="身高(cm)" width="100" />
      <el-table-column prop="bmi" label="BMI" width="100" />
      <el-table-column prop="systolicPressure" label="收缩压" width="100" />
      <el-table-column prop="diastolicPressure" label="舒张压" width="100" />
      <el-table-column prop="heartRate" label="心率" width="100" />
      <el-table-column prop="steps" label="步数" width="100" />
      <el-table-column prop="sleepHours" label="睡眠(h)" width="100" />
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

    <!-- 弹窗：新增/编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑数据' : '添加数据'"
      width="500px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="记录日期" required>
          <el-date-picker
            v-model="form.recordDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="体重(kg)" required>
          <el-input-number
            v-model="form.weight"
            :precision="2"
            :step="0.1"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="身高(cm)" required>
          <el-input-number
            v-model="form.height"
            :precision="2"
            :step="0.5"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="BMI" required>
          <el-input-number
            v-model="form.bmi"
            :precision="2"
            :step="0.1"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="收缩压" required>
          <el-input-number
            v-model="form.systolicPressure"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="舒张压" required>
          <el-input-number
            v-model="form.diastolicPressure"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="心率" required>
          <el-input-number
            v-model="form.heartRate"
            :min="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="步数" required>
          <el-input-number v-model="form.steps" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="睡眠时长(h)" required>
          <el-input-number
            v-model="form.sleepHours"
            :precision="1"
            :step="0.5"
            :min="0"
            :max="24"
            style="width: 100%"
          />
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
import request from "@/utils/request"; // 你的 axios 封装

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const dialogVisible = ref(false);

// 查询参数（注意：不需要 userId，后端从 token 里拿）
const queryParams = reactive({
  pageNumber: 1,
  pageSize: 10,
  startDate: "",
  endDate: "",
});

// 表单数据
const form = reactive({
  id: null,
  recordDate: "",
  weight: null,
  height: null,
  bmi: null,
  systolicPressure: null,
  diastolicPressure: null,
  heartRate: null,
  steps: null,
  sleepHours: null,
});

// 获取数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.post("/health-data/list", queryParams);
    tableData.value = res.list;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

// 重置查询
const resetQuery = () => {
  queryParams.startDate = "";
  queryParams.endDate = "";
  queryParams.pageNumber = 1;
  fetchData();
};

// 打开弹窗
const openDialog = (row = null) => {
  if (row) {
    Object.assign(form, row);
  } else {
    Object.assign(form, {
      id: null,
      recordDate: "",
      weight: null,
      height: null,
      bmi: null,
      systolicPressure: null,
      diastolicPressure: null,
      heartRate: null,
      steps: null,
      sleepHours: null,
    });
  }
  dialogVisible.value = true;
};

// 提交
const submitForm = async () => {
  if (!form.recordDate || !form.weight || !form.height) {
    return ElMessage.warning("请填写完整必填项（日期、体重、身高）");
  }

  // 若未填 BMI，根据身高体重自动计算
  if (!form.bmi && form.weight && form.height) {
    const h = form.height / 100;
    form.bmi = parseFloat((form.weight / (h * h)).toFixed(2));
  }

  if (form.id) {
    await request.post("/health-data/update", form);
    ElMessage.success("更新成功");
  } else {
    await request.post("/health-data/add", form);
    ElMessage.success("添加成功");
  }
  dialogVisible.value = false;
  fetchData();
};

// 删除
const handleDelete = (id) => {
  ElMessageBox.confirm("确定删除该条健康数据吗？", "提示", {
    type: "warning",
  }).then(async () => {
    await request.post(`/health-data/del?id=${id}`);
    ElMessage.success("删除成功");
    fetchData();
  });
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.health-data-container {
  padding: 20px;
  background: #fff;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.filter-box {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
