<template>
  <div class="navbar">
    <div class="flex-box">
      <el-button type="primary" @click="handleCollapsed">
        <el-icon><Expand /></el-icon>
      </el-button>
      <p class="page-title">导航栏</p>
    </div>

    <el-dropdown @command="handleCommand">
      <div class="flex-box">
        <el-avatar
          src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
        />
        <p>用户</p>
        <el-icon><ArrowDown /></el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="1">登录</el-dropdown-item>
          <el-dropdown-item command="2">切换账号</el-dropdown-item>
          <el-dropdown-item command="3">设置</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useAdminStore } from "@/stores/admin";
const command = ref("");
const handleCommand = (command) => {
  console.log(command);
  if (command === "1") {
    router.push("/login");
  }
  if (command === "2") {
    router.push("/switch-account");
  }
  if (command === "3") {
    router.push("/setting");
  }
};
const handleCollapsed = () => {
  useAdminStore().toggleCollapsed();
};
</script>

<style lang="scss" scoped>
.navbar {
  height: 100%;
  background-color: white;
  justify-content: space-between;
  display: flex;
  padding: 0 15px;
  border-bottom: 1px solid #f5f7fa;
  .flex-box {
    display: flex;
    align-items: center;
    justify-content: flex-start;
  }
  .page-title {
    font-size: 26px;
    font-weight: bold;
    margin-left: 20px;
    color: #303133;
    margin-top: 0;
    margin-left: 12px;
  }
}
</style>
