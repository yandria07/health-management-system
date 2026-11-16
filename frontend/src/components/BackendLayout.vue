<template>
  <div class="backend-layout">
    <el-container class="main-container">
      <Sidebar />
      <el-container>
        <el-header>
          <Navbar />
        </el-header>
        <el-main>
          <router-view class="content-container"></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import Sidebar from "./Sidebar.vue";
import Navbar from "./Navbar.vue";
</script>

<style lang="scss" scoped>
.backend-layout {
  height: 100vh;
  .el-header {
    height: 74px !important;
  }
  .main-container {
    height: 100%; //继承父级的高度
    .content-container {
      padding: 20px;
      background-color: rgb(245, 230, 230);
      min-height: calc(100% - 74px);
      height: auto;
    }
  }
}
:deep(.el-menu) {
  --el-menu-bg-color: rgb(241, 191, 191);   // 背景色
  --el-menu-text-color: #9f1d1d;            // 菜单文字：深红色（比纯大红更有质感）
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.45); // 悬浮：半透明白
  --el-menu-hover-text-color: #7f1010;      // 悬浮文字：更深的红
  --el-menu-active-color: #7f1010;          // 选中文字色
  --el-menu-item-height: 54px;              // 菜单项高度
  border-right: none;
}

// 菜单项：更大字号 + 项与项之间的间距 + 圆角药丸形态
:deep(.el-menu-item) {
  font-size: 16px;            // 字体变大（默认 14px）
  margin: 8px 14px;           // 各栏之间的间距
  border-radius: 10px;
  font-weight: 500;
  letter-spacing: 0.5px;
  transition: all 0.25s ease;

  .el-icon {
    font-size: 19px;          // 图标同步放大
  }

  &:hover {
    transform: translateX(3px); // 轻微右移，增加灵动感
  }

  // 选中项：白色药丸 + 深红粗体，突出当前页面
  &.is-active {
    background-color: #fff;
    color: #7f1010;
    font-weight: 700;
    box-shadow: 0 2px 8px rgba(159, 29, 29, 0.18);
  }
}

// 折叠状态下收窄左右留白，避免图标被裁切
:deep(.el-menu--collapse .el-menu-item) {
  margin: 8px auto;
  width: calc(100% - 16px);
}
</style>
