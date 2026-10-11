<template>
  <el-form ref="formRef" :model="formData" class="table-search">
    <el-row :gutter="24">
      <template v-for="item in formItemAttrs" :key="item.prop">
        <el-col v-bind="item.col">
          <el-form-item :label="item.label" :prop="item.prop">
            <!-- ① 输入框 -->
            <el-input
              v-if="item.comp === 'input'"
              v-model="formData[item.prop]"
              :placeholder="item.placeholder || '请输入'"
              clearable
              style="width: 100%"
            />

            <!-- ② 下拉选择 -->
            <el-select
              v-else-if="item.comp === 'select'"
              v-model="formData[item.prop]"
              :placeholder="item.placeholder || '请选择'"
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="opt in item.options || []"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>

            <!-- ③ 日期 / 日期范围选择器（⭐ 这次要的：2 个独立日期框） -->
            <el-date-picker
              v-else-if="
                item.comp === 'date' ||
                item.comp === 'date-picker' ||
                item.comp === 'date-range'
              "
              v-model="formData[item.prop]"
              :type="item.type || 'date'"
              :placeholder="
                item.placeholder ||
                (item.type === 'daterange' ? '选择日期范围' : '选择日期')
              "
              :start-placeholder="item.startPlaceholder || '开始日期'"
              :end-placeholder="item.endPlaceholder || '结束日期'"
              :format="item.format || 'YYYY-MM-DD'"
              :value-format="item.valueFormat || 'YYYY-MM-DD'"
              :range-separator="item.rangeSeparator || '至'"
              :clearable="item.clearable !== false"
              v-bind="item.extraAttrs || {}"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </template>

      <!-- 查询/重置按钮 -->
      <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="6">
        <el-form-item label-width="0">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed, watch } from "vue";

const props = defineProps({
  formItem: {
    type: Array,
    default: () => [],
  },
});
const emit = defineEmits(["search", "reset"]);

const formItemAttrs = computed(() => {
  return props.formItem.map((item) => ({
    ...item,
    col: item.col || { xs: 24, sm: 12, md: 8, lg: 6, xl: 6 },
  }));
});

// 根据 formItem 初始化 formData，避免 v-model 绑定 undefined
const initFormData = () => {
  const obj = {};
  props.formItem.forEach((item) => {
    if (item.type === "daterange" || item.type === "datetimerange") {
      obj[item.prop] = null; // 日期范围默认 null
    } else {
      obj[item.prop] = item.defaultValue ?? "";
    }
  });
  return obj;
};

const formRef = ref(null);
const formData = reactive(initFormData());

// formItem 变化时同步初始化缺失字段
watch(
  () => props.formItem,
  () => {
    const fresh = initFormData();
    Object.keys(fresh).forEach((k) => {
      if (!(k in formData)) formData[k] = fresh[k];
    });
  },
  { deep: true },
);

const handleSearch = () => {
  const payload = JSON.parse(JSON.stringify(formData));

  // ⭐ 核心：如果配置了 splitDateRangeProp，
  //    把选好的日期范围数组 [start, end] 自动拆成 startDate + endDate 两个独立字段（后端最常用）
  props.formItem.forEach((item) => {
    if (
      (item.type === "daterange" || item.type === "datetimerange") &&
      item.splitDateRangeProp === true
    ) {
      const range = payload[item.prop];
      if (Array.isArray(range) && range.length === 2) {
        payload[item.startProp || "startDate"] = range[0];
        payload[item.endProp || "endDate"] = range[1];
      }
      // 删除原数组字段，只留拆分后的 2 个字段
      delete payload[item.prop];
    }
  });

  console.log("🔍 TableSearch 查询参数：", payload);
  emit("search", payload);
};

const handleReset = () => {
  if (formRef.value) formRef.value.resetFields();
  const fresh = initFormData();
  Object.keys(fresh).forEach((k) => {
    formData[k] = fresh[k];
  });
  emit("reset", { ...formData });
};
</script>

<style scoped lang="scss">
.table-search {
  background: #fff;
  padding: 18px 20px 4px;
  border-radius: 4px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);

  .el-date-editor {
    width: 100% !important;
  }
}
</style>
