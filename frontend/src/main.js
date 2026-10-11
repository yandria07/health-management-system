//1.引用
import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router'   
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
//2.创建应用实例
const app = createApp(App)
const pinia=createPinia()
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
//3.挂载应用实例
app.use(router).use(ElementPlus).use(pinia).mount('#app')