<template>
  <div class="profile-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab 1：修改基本信息 -->
      <el-tab-pane label="基本资料" name="base">
        <el-form :model="baseForm" label-width="100px" style="max-width: 500px; margin-top: 20px">
          <el-form-item label="头像">
            <el-upload
              class="avatar-uploader"
              action="/api/admin/upload-avatar"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
            >
              <img v-if="baseForm.avatar" :src="baseForm.avatar" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="baseForm.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="baseForm.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="baseForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="baseForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updateBase">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab 2：修改密码 -->
      <el-tab-pane label="修改密码" name="pwd">
        <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" style="max-width: 500px; margin-top: 20px">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="updatePwd">确认修改</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const activeTab = ref('base')
const pwdFormRef = ref(null)

// 头像上传时，需要带 token
const uploadHeaders = {
  token: localStorage.getItem('token')
}

// 基本信息表单
const baseForm = reactive({
  id: null,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

// 密码表单
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码校验规则
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 加载当前用户信息
const loadUserInfo = async () => {
  const res = await request.get('/admin/info')
  Object.assign(baseForm, res)
  // 同步更新 localStorage
  localStorage.setItem('userInfo', JSON.stringify(res))
}

// 保存基本信息
const updateBase = async () => {
  await request.post('/admin/update-info', baseForm)
  ElMessage.success('修改成功')
  loadUserInfo()
}

// 修改密码
const updatePwd = async () => {
  const valid = await pwdFormRef.value.validate()
  if (!valid) return
  await request.post('/admin/update-pwd', pwdForm)
  ElMessage.success('密码修改成功，请重新登录')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  setTimeout(() => {
    window.location.href = '/auth/login'
  }, 800)
}

// 头像上传成功
const handleAvatarSuccess = (res) => {
  if (res.code === '200') {
    baseForm.avatar = res.data
    ElMessage.success('头像上传成功')
    loadUserInfo()
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

// 上传前校验
const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) ElMessage.error('只能上传图片')
  if (!isLt2M) ElMessage.error('图片大小不能超过 2MB')
  return isImage && isLt2M
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 60px);
}
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  width: 100px;
  height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
}
.avatar {
  width: 100px;
  height: 100px;
  border-radius: 6px;
  object-fit: cover;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}
</style>