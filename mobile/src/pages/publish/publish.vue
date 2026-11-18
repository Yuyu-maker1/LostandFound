<template>
  <view class="publish-page">
    <view class="intro">
      <text class="intro-title">信息发布</text>
      <text class="intro-sub">请选择登记类型</text>
    </view>

    <view class="card-list">
      <view class="type-card" @click="goFound">
        <view class="type-icon found-icon">拾</view>
        <view class="type-content">
          <view class="type-title-row">
            <text class="type-title">拾获物品登记</text>
            <text v-if="isAttendant" class="official-tag">官方认证</text>
          </view>
          <text class="type-sub">捡到物品，发布招领信息</text>
          <text class="type-desc">登记后等待管理员审核，通过后供失主查找</text>
        </view>
        <text class="type-arrow">›</text>
      </view>

      <view
        class="type-card"
        :class="{ disabled: isAttendant }"
        @click="goLost"
      >
        <view class="type-icon lost-icon">寻</view>
        <view class="type-content">
          <view class="type-title-row">
            <text class="type-title">遗失物品登记</text>
            <text v-if="isAttendant" class="disabled-tag">仅旅客账号可发布</text>
          </view>
          <text class="type-sub">丢失物品，发布寻物启事</text>
          <text class="type-desc">填写乘车行程和物品特征，提交后等待审核</text>
        </view>
        <text class="type-arrow">›</text>
      </view>
    </view>

    <view class="notice">
      <text class="notice-title">温馨提示</text>
      <text class="notice-line">1. 拾获物品请尽量移交车站服务台或乘务员，便于规范保管与交接。</text>
      <text class="notice-line">2. 发布信息须经管理员审核，审核通过后才会在前台展示。</text>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'

const userInfo = ref(uni.getStorageSync('userInfo') || {})
const isAttendant = computed(() => userInfo.value.role === 'attendant')

function goFound() {
  uni.navigateTo({ url: '/pages/publish/found' })
}

function goLost() {
  if (isAttendant.value) {
    uni.showModal({
      title: '提示',
      content: '乘务员工作账号不能发布寻物启事，请使用个人旅客账号',
      showCancel: false,
      confirmText: '知道了'
    })
    return
  }
  uni.navigateTo({ url: '/pages/publish/lost' })
}
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.intro {
  padding: 40rpx 32rpx 30rpx;
  background: linear-gradient(135deg, #1677ff, #4f9bff);
  color: #ffffff;
}

.intro-title {
  display: block;
  font-size: 42rpx;
  font-weight: 600;
}

.intro-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.86);
}

.card-list {
  margin: 24rpx 32rpx 0;
}

.type-card {
  display: flex;
  align-items: center;
  margin-bottom: 22rpx;
  padding: 32rpx 28rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 30rpx rgba(15, 23, 42, 0.05);
}

.type-card.disabled {
  opacity: 0.55;
}

.type-icon {
  flex-shrink: 0;
  width: 92rpx;
  height: 92rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
  border-radius: 24rpx;
  font-size: 38rpx;
  font-weight: 600;
  color: #ffffff;
}

.found-icon {
  background: linear-gradient(135deg, #1677ff, #4f9bff);
}

.lost-icon {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
}

.type-content {
  flex: 1;
  min-width: 0;
}

.type-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.type-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.official-tag {
  margin-left: 12rpx;
  padding: 2rpx 10rpx;
  background: #dcfce7;
  color: #15803d;
  border-radius: 6rpx;
  font-size: 20rpx;
}

.disabled-tag {
  margin-left: 12rpx;
  padding: 2rpx 10rpx;
  background: #f1f5f9;
  color: #94a3b8;
  border-radius: 6rpx;
  font-size: 20rpx;
}

.type-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #475569;
}

.type-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: #94a3b8;
}

.type-arrow {
  flex-shrink: 0;
  margin-left: 12rpx;
  color: #cbd5e1;
  font-size: 40rpx;
}

.notice {
  margin: 12rpx 32rpx 0;
  padding: 24rpx;
  background: #fffdf5;
  border: 1rpx solid #f2e6c8;
  border-radius: 18rpx;
}

.notice-title {
  display: block;
  margin-bottom: 10rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #8a6d1f;
}

.notice-line {
  display: block;
  font-size: 23rpx;
  line-height: 1.8;
  color: #7d6f52;
}
</style>
