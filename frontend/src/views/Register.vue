<template>
  <div class="container">
    <div class="title">
      <div class="title-text">
        <h2>创建您的用户</h2>
        <p>请填写以下信息注册账号</p>
      </div>
    </div>
    <div class="form-container">
      <el-form
        label-position="top"
        :model="formData"
        :rules="rules"
        ref="submitFormRef"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            size="large"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="formData.email"
            placeholder="请输入邮箱"
            size="large"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="formData.nickname"
            placeholder="请输入昵称"
            size="large"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="请输入手机号"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            placeholder="请输入密码"
            size="large"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            placeholder="请输入确认密码"
            size="large"
            type="password"
            show-password
          />
        </el-form-item>
        <el-button
          class="btn"
          type="primary"
          size="large"
          @click="submitForm(submitFormRef)"
          >确认注册</el-button
        >
      </el-form>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { register } from "@/api/frontend";
import { ElMessage } from "element-plus";
const submitFormRef = ref(null);
const router = useRouter();
const formData = reactive({
  username: "",
  email: "",
  nickname: "",
  phone: "",
  password: "",
  confirmPassword: "",
  gender: 0,
  userType: 1,
});
const rules = reactive({
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  email: [{ required: true, message: "请输入邮箱", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  confirmPassword: [{ required: true, message: "请确认密码", trigger: "blur" }],
});
const submitForm = async () => {
  if (!submitFormRef.value) return;

  try {
    // 1. 前端校验（formData 里依然包含 confirmPassword，用于校验密码一致性）
    const valid = await submitFormRef.value.validate();
    if (!valid) return;

    // 2. 核心：解构赋值，剥离 confirmPassword，剩下的就是 payload
    const { confirmPassword, ...payload } = formData;

    // 3. 发送 payload（不包含 confirmPassword）
    await register(payload);

    ElMessage.success("注册成功");
    router.push("/auth/login");
  } catch (error) {
    console.error("注册失败:", error);
  }
};
</script>
<style>
.container {
  width: 384px;
  .flex-box {
    display: flex;
    align-items: center;
  }
  .title {
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
    .btn {
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
