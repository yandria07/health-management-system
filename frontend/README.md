# 个人健康管理平台（前端）

基于 **Vue 3 + Vite + Element Plus** 的单页应用，提供健康数据管理、心理健康测评、健康计划、AI 健康分析与对话、内容推荐、体检报告解读等功能。

> 本前端需配合同仓库的 Spring Boot 后端（`../backend`）一起运行，接口经 Vite 代理转发到后端 `http://localhost:8080`。

---

## 1. 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3（`<script setup>` SFC） | ^3.5.42 |
| 构建工具 | Vite | ^8.3.0 |
| 路由 | vue-router | ^4.6.4 |
| 状态管理 | Pinia | ^4.0.3 |
| UI 组件库 | Element Plus + @element-plus/icons-vue | ^2.14.5 |
| 图表 | ECharts | ^6.1.0 |
| HTTP 客户端 | axios | ^1.20.0 |
| 样式预处理 | Sass | ^1.97.2 |

---

## 2. 环境要求

- **Node.js**：建议 v18+（当前环境为 v22）
- **npm**：随 Node.js 安装
- **后端服务**：Spring Boot 应用，监听 `8080` 端口（见文末「后端配合」）
- **MySQL**：后端依赖的数据库服务需处于运行状态

---

## 3. 快速开始

### 3.1 安装依赖

```bash
cd frontend
npm install
```

### 3.2 启动开发服务器

```bash
npm run dev
```

启动后控制台会显示访问地址（默认）：

```
http://localhost:5173/
```

> `vite.config.js` 中配置了 `server.open = true`，启动时会尝试自动打开浏览器。
> 若 5173 端口被占用，Vite 会自动顺延到下一个可用端口，请以控制台输出为准。

### 3.3 生产构建

```bash
npm run build
```

产物输出到 `dist/` 目录，可用于静态部署。

### 3.4 本地预览构建产物

```bash
npm run preview
```

---

## 4. 目录结构

```
frontend/
├── index.html               # HTML 入口
├── vite.config.js           # Vite 配置（@ 别名、/api 代理、自动打开浏览器）
├── package.json             # 依赖与脚本
├── src/
│   ├── main.js              # 应用入口：注册路由、Pinia、Element Plus、全局图标
│   ├── App.vue              # 根组件
│   ├── style.css            # 全局样式
│   ├── router/
│   │   └── index.js         # 路由表（/back 后台页 + /auth 登录注册页）
│   ├── api/                 # 接口封装（按业务模块划分）
│   │   ├── admin.js         # 登录、分类树
│   │   ├── frontend.js      # 注册
│   │   ├── healthPlan.js    # 健康计划、打卡、提醒
│   │   └── physicalExam.js  # 体检报告、AI 解读
│   ├── views/               # 页面组件
│   │   ├── Dashboard.vue        # 数据看板
│   │   ├── Knowledge.vue        # 健康数据录入
│   │   ├── MentalHealth.vue     # 心理健康测评
│   │   ├── Schedule.vue         # 制定计划 + 待做提醒
│   │   ├── Consulation.vue      # AI 健康分析/建议
│   │   ├── Chat.vue             # AI 多轮对话（历史持久化）
│   │   ├── ContentRecommend.vue # 内容推荐
│   │   ├── PhysicalExam.vue     # 体检报告录入/详情 + AI 解读
│   │   ├── Profile.vue          # 个人中心
│   │   ├── Login.vue            # 登录
│   │   └── Register.vue         # 注册
│   ├── components/          # 布局与通用组件
│   │   ├── BackendLayout.vue   # 后台整体布局
│   │   ├── AuthLayout.vue      # 登录注册布局
│   │   ├── Sidebar.vue         # 侧边栏菜单（由路由自动生成）
│   │   ├── Navbar.vue          # 顶部导航
│   │   ├── PageHead.vue        # 页头
│   │   └── TableSearch.vue     # 表格搜索
│   ├── stores/
│   │   └── admin.js         # Pinia store（侧边栏折叠状态）
│   ├── utils/
│   │   ├── request.js       # axios 封装（baseURL=/api、token 注入、响应拦截）
│   │   ├── mentalHealth.js  # 心理测评量表定义、计分、历史记录（localStorage）
│   │   └── recommendEngine.js # 推荐引擎（需求标签 → 内容匹配）
│   └── data/
│       └── recommendContent.js # 内容推荐静态内容库
```

---

## 5. 路由与菜单

路由定义在 `src/router/index.js`，分两组：

- **`/auth`**：`login`（登录）、`register`（注册），使用 `AuthLayout` 布局。
- **`/back`**：后台功能页，使用 `BackendLayout` 布局。

侧边栏菜单（`components/Sidebar.vue`）会**自动遍历 `/back` 的子路由**生成菜单项，新增页面时只需在路由表里加一项并配置 `meta.title` 与 `meta.icon`，无需改动菜单组件：

```js
{
  path: 'physical-exam',
  component: () => import('@/views/PhysicalExam.vue'),
  meta: { title: '体检报告', icon: 'Files' }
}
```

---

## 6. 接口请求说明

所有 HTTP 请求统一通过 `src/utils/request.js` 创建的 axios 实例发出：

- **baseURL**：`/api`，由 Vite 代理转发到 `http://localhost:8080`（后端 context-path 为 `/api`，即实际请求 `/api/xxx` → 后端 `localhost:8080/api/xxx`）。
- **请求拦截**：自动从 `localStorage` 读取 `token` 并写入请求头 `token`。
- **响应拦截**：
  - `code === 200`：直接返回 `data.data`；
  - `code === -1`：登录过期，清除 token/userInfo 并跳转登录页；
  - 其他 code：弹出错误提示并 reject。

> 注意：AI 相关接口（对话、体检解读）耗时较长，在 `api/` 中对这些请求单独设置了更长的超时（`60000`/`90000` 毫秒），其余接口保持默认 5 秒。

---

## 7. 功能模块一览

| 页面 | 路径 | 说明 |
|------|------|------|
| 数据看板 | `/back/dashboard` | 健康趋势图表、关键指标概览 |
| 健康数据 | `/back/knowledge` | 体重/BMI/血压/心率/睡眠等数据录入与列表 |
| 心理健康测评 | `/back/mental-health` | PHQ-9、GAD-7 等量表自评、情绪健康分、长期低谷监测（默认折叠问卷） |
| 制定计划 | `/back/schedule` | 健康计划 CRUD、打卡日历、**置顶待做提醒**（可收起、可删除、到点通知） |
| AI 分析 | `/back/consultation` | 生成健康建议、历史建议、一键导入计划、跳转对话 |
| AI 对话 | `/back/ai-chat` | 多轮对话，历史记录持久化到数据库 |
| 内容推荐 | `/back/content-recommend` | 按健康数据个性化推荐视频/音乐/图文，点击卡片外链跳转 |
| 体检报告 | `/back/physical-exam` | 22 项体检指标录入、绿正常/红异常标记、AI 解读（独立表存储） |
| 个人中心 | `/back/profile` | 账号资料维护（邮箱用于接收提醒邮件） |

---

## 8. 后端配合（必读）

前端依赖后端提供数据接口，启动前端前请确认后端已启动：

1. 后端目录：`../backend`，Spring Boot 项目（Java 17+，依赖 MySQL）。
2. 后端默认端口 **8080**，context-path 为 `/api`。
3. 数据库：MySQL 需运行，且后端配置中的数据库（`student`）可用。
4. 后端启动示例（在 `backend` 目录执行，含邮件提醒的环境变量）：

   ```bash
   MAIL_USERNAME=你的QQ邮箱@qq.com MAIL_PASSWORD=你的SMTP授权码 \
     java -jar target/health-server-0.0.1-SNAPSHOT.jar --server.port=8080
   ```

   > 邮件提醒为可选功能：不配 `MAIL_USERNAME/MAIL_PASSWORD` 后端也能启动，只是无法发送提醒邮件。
   > 后端 `application.yml` 中原有端口 `28358` 与本机其他进程冲突，因此用 `--server.port=8080` 覆盖，与前端代理保持一致。

5. Vite 代理配置位于 `vite.config.js`：

   ```js
   server: {
     proxy: {
       '/api': { target: 'http://localhost:8080', changeOrigin: true }
     }
   }
   ```

   修改后端地址时，改这里的 `target` 即可。

---

## 9. 登录说明

- 登录接口：`POST /api/admin/login`，请求体 `{ username, password }`。
- 登录成功后 token 存入 `localStorage`，后续请求通过 `token` 请求头携带。
- AI 对话与体检报告等需登录态的接口，均按当前登录用户隔离数据。

---

## 10. 常见问题

**Q：接口请求报 `ECONNREFUSED` 或 502？**
A：后端未启动或端口不对。确认后端在 8080 监听，且 `vite.config.js` 代理 target 与之匹配。

**Q：AI 对话/解读报 `timeout`？**
A：大模型响应较慢。`api/chat` 与 `api/physicalExam.js` 已单独设置 60/90 秒超时；若仍超时，请检查后端到智谱 API 的网络与 API Key 配置。

**Q：侧边栏新增页面后没显示菜单？**
A：菜单由路由自动生成。确认新路由加在 `backendRoutes[0].children` 下，且配置了 `meta.title` 和 `meta.icon`。

**Q：构建时提示 chunk 体积过大？**
A：ECharts 等库体积较大，属于正常警告，不影响运行，可忽略或用动态导入优化。
