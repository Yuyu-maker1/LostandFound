<template>
  <view class="mine-page">
    <view class="profile-card">
      <view class="avatar-wrap" @click="onChooseAvatar">
        <image
          v-if="avatarUrl"
          :src="avatarUrl"
          class="avatar-img"
          mode="aspectFill"
        />
        <view v-else class="avatar">{{ avatarText }}</view>
        <view class="avatar-mask">{{ uploading ? '上传中' : '更换' }}</view>
      </view>
      <view class="profile-info">
        <text class="profile-name">{{ userInfo.realName || userInfo.name || '未登录' }}</text>
        <text class="profile-role">{{ roleText }}</text>
      </view>
    </view>

    <view class="menu-card">
      <view class="menu-item" @click="pwdVisible = !pwdVisible">
        <text>修改密码</text>
        <text class="menu-status">{{ pwdVisible ? '收起' : '去修改' }}</text>
      </view>
      <view class="menu-item" @click="goMyPublish">
        <text>我的发布</text>
        <text class="menu-status">查看 ›</text>
      </view>
      <view class="menu-item">
        <text>我的认领</text>
        <text class="menu-status">开发中</text>
      </view>
      <view class="menu-item">
        <text>站内消息</text>
        <text class="menu-status">开发中</text>
      </view>
    </view>

    <view v-if="pwdVisible" class="pwd-card">
      <view class="field">
        <text class="field-label">原密码</text>
        <input
          v-model="pwdForm.oldPwd"
          class="field-input"
          password
          placeholder="请输入原密码"
          placeholder-class="placeholder"
          maxlength="20"
        />
      </view>
      <view class="field">
        <text class="field-label">新密码</text>
        <input
          v-model="pwdForm.newPwd"
          class="field-input"
          password
          placeholder="6-20 位，需含字母和数字"
          placeholder-class="placeholder"
          maxlength="20"
        />
      </view>
      <view class="field">
        <text class="field-label">确认新密码</text>
        <input
          v-model="pwdForm.confirmPwd"
          class="field-input"
          password
          placeholder="请再次输入新密码"
          placeholder-class="placeholder"
          maxlength="20"
        />
      </view>
      <text v-if="pwdError" class="error-text">{{ pwdError }}</text>
      <button
        class="submit-btn"
        :loading="pwdSubmitting"
        :disabled="pwdSubmitting"
        @click="onChangePassword"
      >
        {{ pwdSubmitting ? '提交中...' : '确认修改' }}
      </button>
    </view>

    <button class="logout-btn" @click="onLogout">退出登录</button>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { BASE_URL, clearLogin, post, uploadFile } from '../../utils/request'

const userInfo = ref(uni.getStorageSync('userInfo') || {})
const uploading = ref(false)
const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdError = ref('')

const pwdForm = reactive({
  oldPwd: '',
  newPwd: '',
  confirmPwd: ''
})

const roleText = computed(() => {
  if (userInfo.value.role === 'attendant') return '乘务员'
  if (userInfo.value.role === 'user') return '旅客'
  return ''
})

const avatarUrl = computed(() => {
  const img = userInfo.value.userimg
  if (!img) return ''
  return img.startsWith('http') ? img : BASE_URL + img
})

const avatarText = computed(() => {
  const name = userInfo.value.realName || userInfo.value.name || '客'
  return String(name).slice(0, 1)
})

function onChooseAvatar() {
  if (uploading.value) return
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = res.tempFilePaths && res.tempFilePaths[0]
      if (!filePath) return
      uploading.value = true
      uni.showLoading({ title: '上传中...' })
      try {
        const data = await uploadFile('/app/userImg', filePath)
        const img = (data && data.data1) || ''
        userInfo.value = { ...userInfo.value, userimg: img }
        uni.setStorageSync('userInfo', userInfo.value)
        uni.showToast({ title: '头像已更新', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
        uploading.value = false
      }
    }
  })
}

async function onChangePassword() {
  if (pwdSubmitting.value) return
  pwdError.value = ''

  if (!pwdForm.oldPwd) {
    pwdError.value = '请输入原密码'
    return
  }
  if (pwdForm.newPwd.length < 6 || pwdForm.newPwd.length > 20) {
    pwdError.value = '新密码长度需为 6-20 位'
    return
  }
  if (!/[A-Za-z]/.test(pwdForm.newPwd) || !/\d/.test(pwdForm.newPwd)) {
    pwdError.value = '新密码需同时包含字母和数字'
    return
  }
  if (pwdForm.confirmPwd !== pwdForm.newPwd) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }

  pwdSubmitting.value = true
  try {
    await post('/app/password', {
      oldPwd: pwdForm.oldPwd,
      newPwd: pwdForm.newPwd
    })
    clearLogin()
    uni.showToast({ title: '密码已修改，请重新登录', icon: 'none', duration: 1600 })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/login/login' })
    }, 1500)
  } catch (e) {
    pwdError.value = e.message || '密码修改失败'
  } finally {
    pwdSubmitting.value = false
  }
}

function onLogout() {
  clearLogin()
  uni.reLaunch({ url: '/pages/login/login' })
}

function goMyPublish() {
  uni.navigateTo({ url: '/pages/mine/publish' })
}
</script>

<style scoped>
.mine-page {
  min-height: 100vh;
  padding: 40rpx 32rpx;
}

.profile-card {
  display: flex;
  align-items: center;
  padding: 40rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #1677ff, #4f9bff);
  color: #ffffff;
}

.avatar-wrap {
  position: relative;
  width: 110rpx;
  height: 110rpx;
  margin-right: 24rpx;
  border-radius: 55rpx;
  overflow: hidden;
}

.avatar-img {
  width: 110rpx;
  height: 110rpx;
  border-radius: 55rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.5);
}

.avatar {
  width: 110rpx;
  height: 110rpx;
  border-radius: 55rpx;
  background: rgba(255, 255, 255, 0.22);
  border: 2rpx solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  font-weight: 600;
}

.avatar-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 34rpx;
  line-height: 34rpx;
  text-align: center;
  font-size: 20rpx;
  color: #ffffff;
  background: rgba(15, 23, 42, 0.45);
}

.profile-name {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
}

.profile-role {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.82);
}

.menu-card {
  margin-top: 28rpx;
  border-radius: 24rpx;
  background: #ffffff;
  padding: 0 28rpx;
}

.menu-item {
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #eef2f7;
  font-size: 28rpx;
  color: #1e293b;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-status {
  font-size: 24rpx;
  color: #94a3b8;
}

.pwd-card {
  margin-top: 28rpx;
  border-radius: 24rpx;
  background: #ffffff;
  padding: 32rpx 28rpx 20rpx;
}

.field {
  margin-bottom: 24rpx;
}

.field-label {
  display: block;
  font-size: 26rpx;
  color: #475569;
  margin-bottom: 12rpx;
}

.field-input {
  width: 100%;
  height: 88rpx;
  padding: 0 28rpx;
  background: #f5f7fb;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #1f2937;
}

.placeholder {
  color: #a8b3c4;
}

.error-text {
  display: block;
  margin-bottom: 16rpx;
  font-size: 24rpx;
  color: #e5484d;
}

.submit-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  background: linear-gradient(90deg, #1677ff, #4f9bff);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}

.submit-btn[disabled] {
  opacity: 0.7;
}

.logout-btn {
  margin-top: 40rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  background: #ffffff;
  color: #e5484d;
  font-size: 28rpx;
}
</style>
