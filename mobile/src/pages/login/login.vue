<template>
  <view class="login-page">
    <view class="brand">
      <view class="brand-icon">失</view>
      <text class="brand-title">高铁失物招领</text>
      <text class="brand-sub">找回物品，从一次登录开始</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">账号 / 手机号</text>
        <input
          v-model="account"
          class="field-input"
          type="text"
          placeholder="请输入账号或手机号"
          placeholder-class="placeholder"
          maxlength="20"
          confirm-type="next"
        />
      </view>

      <view class="field">
        <text class="field-label">密码</text>
        <view class="pwd-row">
          <input
            v-model="pwd"
            class="field-input pwd-input"
            :password="!pwdVisible"
            placeholder="请输入密码"
            placeholder-class="placeholder"
            maxlength="20"
            confirm-type="done"
            @confirm="onLogin"
          />
          <text class="pwd-toggle" @click="pwdVisible = !pwdVisible">
            {{ pwdVisible ? '隐藏' : '显示' }}
          </text>
        </view>
      </view>

      <text v-if="errorText" class="error-text">{{ errorText }}</text>

      <button class="login-btn" :loading="loading" :disabled="loading" @click="onLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </button>

      <view class="tips">
        <text>登录即表示同意车站失物招领相关规定</text>
      </view>
    </view>

    <view class="bottom">
      <text class="bottom-text">没有账号？</text>
      <text class="bottom-link" @click="onGoRegister">立即注册</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { post } from '../../utils/request'

const account = ref('')
const pwd = ref('')
const pwdVisible = ref(false)
const loading = ref(false)
const errorText = ref('')

function toast(title) {
  uni.showToast({ title, icon: 'none', duration: 2000 })
}

function validate() {
  if (!account.value.trim()) {
    errorText.value = '请输入账号或手机号'
    return false
  }
  if (account.value.trim().length < 3) {
    errorText.value = '账号长度至少 3 位'
    return false
  }
  if (!pwd.value) {
    errorText.value = '请输入密码'
    return false
  }
  if (pwd.value.length < 6) {
    errorText.value = '密码长度至少 6 位'
    return false
  }
  return true
}

async function onLogin() {
  if (loading.value) return
  errorText.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const res = await post('/app/login', {
      name: account.value.trim(),
      pwd: pwd.value
    })
    uni.setStorageSync('token', res.token || '')
    uni.setStorageSync('userInfo', res.data1 || {})
    uni.showToast({ title: '登录成功', icon: 'success', duration: 1200 })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 1000)
  } catch (e) {
    // 直接展示后端 msg，不再统一提示“登录失败”
    errorText.value = e.message || '登录失败，请稍后重试'
    toast(errorText.value)
  } finally {
    loading.value = false
  }
}

function onGoRegister() {
  uni.navigateTo({ url: '/pages/register/register' })
}

// 注册成功后回填刚注册的账号，避免重复输入
onLoad(() => {
  const pendingName = uni.getStorageSync('pendingLoginName')
  if (pendingName) {
    account.value = pendingName
    uni.removeStorageSync('pendingLoginName')
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: calc(var(--status-bar-height, 0px) + 90rpx) 48rpx 60rpx;
  background: linear-gradient(180deg, #1677ff 0%, #4f9bff 38%, #f5f7fb 38%, #f5f7fb 100%);
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #ffffff;
}

.brand-icon {
  width: 112rpx;
  height: 112rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.2);
  border: 2rpx solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 52rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
  margin-bottom: 24rpx;
}

.brand-title {
  font-size: 44rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.brand-sub {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

.form-card {
  margin-top: 64rpx;
  background: #ffffff;
  border-radius: 28rpx;
  padding: 48rpx 40rpx 32rpx;
  box-shadow: 0 20rpx 60rpx rgba(22, 119, 255, 0.16);
}

.field {
  margin-bottom: 34rpx;
}

.field-label {
  display: block;
  font-size: 26rpx;
  color: #475569;
  margin-bottom: 14rpx;
}

.field-input {
  width: 100%;
  height: 92rpx;
  padding: 0 28rpx;
  background: #f5f7fb;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #1f2937;
}

.placeholder {
  color: #a8b3c4;
}

.pwd-row {
  display: flex;
  align-items: center;
  background: #f5f7fb;
  border-radius: 16rpx;
}

.pwd-input {
  flex: 1;
  background: transparent;
}

.pwd-toggle {
  padding: 0 28rpx;
  font-size: 26rpx;
  color: #1677ff;
}

.error-text {
  display: block;
  margin-bottom: 20rpx;
  font-size: 24rpx;
  color: #e5484d;
}

.login-btn {
  width: 100%;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 46rpx;
  background: linear-gradient(90deg, #1677ff, #4f9bff);
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 600;
  letter-spacing: 4rpx;
  border: none;
}

.login-btn[disabled] {
  opacity: 0.7;
}

.tips {
  margin-top: 26rpx;
  text-align: center;
  font-size: 22rpx;
  color: #94a3b8;
}

.bottom {
  margin-top: auto;
  padding-top: 60rpx;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 26rpx;
}

.bottom-text {
  color: #64748b;
}

.bottom-link {
  color: #1677ff;
  margin-left: 8rpx;
}
</style>
