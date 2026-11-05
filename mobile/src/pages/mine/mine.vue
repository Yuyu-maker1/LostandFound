<template>
  <view class="mine-page">
    <view class="profile-card">
      <view class="avatar">{{ avatarText }}</view>
      <view class="profile-info">
        <text class="profile-name">{{ userInfo.realName || userInfo.name || '未登录' }}</text>
        <text class="profile-role">旅客账号</text>
      </view>
    </view>

    <view class="menu-card">
      <view class="menu-item">
        <text>我的发布</text>
        <text class="menu-status">开发中</text>
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

    <button class="logout-btn" @click="onLogout">退出登录</button>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'

const userInfo = ref(uni.getStorageSync('userInfo') || {})

const avatarText = computed(() => {
  const name = userInfo.value.realName || userInfo.value.name || '客'
  return String(name).slice(0, 1)
})

function onLogout() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.reLaunch({ url: '/pages/login/login' })
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
  margin-right: 24rpx;
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
