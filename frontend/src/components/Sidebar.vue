<template>
  <el-aside :width="isCollapsed ? '64px' : '264px'">
    <el-menu
      :collapse="isCollapsed"
      :collapse-transition="false"
      :default-active="activeIndex"
      class="menu-style"
    >
      <div class="brand">
        <el-image style="width: 50px; height: 50px" :src="iconUrl" alt="logo" />
        <div v-show="!isCollapsed" class="info-card">
          <h1 class="brand-title">个人健康管理平台</h1>
          <p class="brand-subtitle">温脉</p>
        </div>
      </div>
      <el-menu-item
        @click="selectMenu(item.path)"
        v-for="item in router.options.routes[0].children"
        :key="item.path"
        :index="item.path"
      >
        <el-icon><component :is="item.meta.icon" /></el-icon>
        <span>{{ item.meta.title }}</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAdminStore } from "@/stores/admin";
const router = useRouter();
const route = useRoute();
const iconUrl = new URL("@/assets/images/机器人.png", import.meta.url).href;
const isCollapsed = computed(() => useAdminStore().isCollapsed);
// 根据当前路由高亮对应菜单项
const activeIndex = computed(() => route.path.replace("/back/", ""));
const selectMenu = (path) => {
  const currentRoute = router.options.routes[0];
  router.push(`${currentRoute.path}/${path}`);
};
</script>

<style lang="scss" scoped>
.menu-style {
  height: 100%;
}
.brand {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 16px;
  gap: 12px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  .info-card {
    flex: 1;
    text-align: left;
    .brand-title {
      font-size: 20px;
      font-weight: bold;
      margin-bottom: 5px;
      color: #7f1010;
      margin-top: 0;
    }
    .brand-subtitle {
      font-size: 15px;
      color: #d94679;
      margin: 0;
    }
  }
}
</style>
