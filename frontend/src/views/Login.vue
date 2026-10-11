<template>
  <div class="container">
    <div class="title">
      <div class="back-home">
        <el-icon class="icon"><Back /></el-icon>
        <span>返回首页</span>
      </div>
      <div class="title-text">
        <h2>登录您的账户</h2>
        <p class="text">登录您的账户，开始管理您的健康</p>
      </div>
    </div>
    <div class="form-container">
      <el-form
        ref="ruleFormRef"
        :model="formData"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            size="large"
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            size="large"
            placeholder="请输入密码"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            class="login-btn"
            size="large"
            type="primary"
            @click="submitForm(ruleFormRef)"
            >登录账户</el-button
          >
        </el-form-item>
      </el-form>
      <div class="footer">
        <p>还没有账户？<router-link to="/auth/register">去注册</router-link></p>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive } from "vue";
import { Back } from "@element-plus/icons-vue";
import { login } from "@/api/admin";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

const router = useRouter();
const formData = reactive({
  username: "",
  password: "",
});
const ruleFormRef = ref(null);
const rules = reactive({
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
});
const submitForm = async () => {
  if (!ruleFormRef.value) return;
  try {
    const valid = await ruleFormRef.value.validate();
    if (!valid) {
      return console.error("表单校验失败");
    }
    const res = await login(formData);
    localStorage.setItem("token", res.token);
    localStorage.setItem("userInfo", JSON.stringify(res.userInfo));
    ElMessage.success("登录成功");
    router.push("/back/dashboard");
  } catch (error) {
    ElMessage.log("登录失败", error.message);
  }
};
</script>
<style scoped lang="scss">
.container {
  width: 384px;

  .title {
    .back-home {
      margin-bottom: 60px;
    }
    .title-text {
      text-align: center;
      h2 {
        font-size: 36px;
        margin-bottom: 10px;
      }
      p {
        font-size: 18px;
        color: #6b7280;
      }
    }
  }
  .form-container {
    margin-top: 30px;
    .login-btn {
      margin-top: 40px;
      width: 100%;
    }
    .footer {
      padding: 30px;
      text-align: center;
    }
  }
}
</style>
