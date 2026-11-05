<template>
  <view class="placeholder-page">
    <view class="placeholder-icon">+</view>
    <text class="placeholder-title">发布功能开发中</text>
    <text class="placeholder-desc">后续在这里发布寻物启事或招领信息</text>
    <button class="back-btn" @click="goHome">返回首页</button>
  </view>
</template>

<script setup>
function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style scoped>
.placeholder-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 60rpx;
}

.placeholder-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 32rpx;
  background: #e0edff;
  color: #1677ff;
  font-size: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28rpx;
}

.placeholder-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #0f172a;
}

.placeholder-desc {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #94a3b8;
}

.back-btn {
  margin-top: 40rpx;
  height: 76rpx;
  line-height: 76rpx;
  padding: 0 48rpx;
  border-radius: 38rpx;
  background: #1677ff;
  color: #ffffff;
  font-size: 28rpx;
}
</style>
