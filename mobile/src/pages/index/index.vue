<template>
  <view class="home">
    <!-- 顶部搜索（首版仅 UI + 占位提示，不做检索逻辑） -->
    <view class="search-wrap">
      <view class="search-box" @click="onSearch">
        <text class="search-icon">⌕</text>
        <text class="search-placeholder">搜物品名称、车次或车站</text>
      </view>
    </view>

    <!-- 公告 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">车站公告</text>
        <text class="section-extra">滚动查看</text>
      </view>
      <scroll-view v-if="announcements.length" class="notice-scroll" scroll-x>
        <view class="notice-row">
          <view v-for="item in announcements" :key="item.noticeId" class="notice-card">
            <text class="notice-tag">公告</text>
            <text class="notice-title">{{ item.title }}</text>
            <text class="notice-content">{{ item.content }}</text>
          </view>
        </view>
      </scroll-view>
      <view v-else class="notice-empty">
        <text>暂无公告</text>
      </view>
    </view>

    <!-- 分类宫格 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">按分类查找</text>
      </view>
      <view class="category-grid">
        <view v-for="(item, index) in categories" :key="item.id" class="category-item" @click="onCategory(item)">
          <view class="category-icon" :class="'icon-' + (index % 5)">{{ item.iconText }}</view>
          <text class="category-name">{{ item.name }}</text>
        </view>
      </view>
    </view>

    <!-- 最新招领 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">最新招领</text>
        <text class="section-extra" @click="load(true)">刷新</text>
      </view>

      <view v-if="loadError" class="state-box">
        <text class="state-title">加载失败</text>
        <text class="state-desc">请检查网络后重试</text>
        <button class="retry-btn" @click="load(true)">重新加载</button>
      </view>

      <view v-else-if="!foundList.length && !loading" class="state-box">
        <text class="state-title">暂无招领信息</text>
        <text class="state-desc">有新线索时会第一时间显示在这里</text>
      </view>

      <view v-else class="found-list">
        <view v-for="item in foundList" :key="item.foundId" class="found-card">
          <view class="found-left">
            <view class="thumb">{{ (item.title || '物').slice(0, 1) }}</view>
          </view>
          <view class="found-body">
            <view class="found-top">
              <text class="found-title">{{ item.title || '未命名物品' }}</text>
              <text class="status-tag" :class="'status-' + item.status">{{ statusText(item.status) }}</text>
            </view>
            <text class="found-meta">车站：{{ item.station || '待补充' }}</text>
            <text class="found-meta">时间：{{ formatTime(item.foundTime) }}</text>
          </view>
        </view>

        <view class="load-more">
          <text v-if="loading">加载中...</text>
          <text v-else-if="foundList.length >= total">已经到底啦</text>
          <text v-else @click="loadMore">上拉加载更多</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app'
import { get } from '../../utils/request'

const announcements = ref([])
const categories = ref([])
const foundList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const loadError = ref(false)

function toast(title) {
  uni.showToast({ title, icon: 'none' })
}

function statusText(status) {
  if (status === 0) return '待认领'
  if (status === 1) return '认领中'
  return '已结束'
}

function formatTime(value) {
  if (!value) return '时间待补充'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function load(reset = false, fromPullDown = false) {
  if (loading.value) {
    if (fromPullDown) uni.stopPullDownRefresh()
    return
  }
  loading.value = true
  loadError.value = false
  const targetPage = reset ? 1 : pageNum.value + 1

  try {
    const res = await get('/app/home', {
      pageNum: targetPage,
      pageSize: pageSize.value
    })
    const data = res.data1 || {}
    announcements.value = data.announcements || []
    categories.value = data.categories || []
    const list = data.foundList || []
    foundList.value = reset ? list : foundList.value.concat(list)
    total.value = Number(data.total || 0)
    pageNum.value = targetPage
  } catch (e) {
    if (reset) {
      loadError.value = true
    } else {
      toast(e.message || '加载失败')
    }
  } finally {
    loading.value = false
    if (fromPullDown) uni.stopPullDownRefresh()
  }
}

function loadMore() {
  if (loading.value) return
  if (foundList.value.length >= total.value) return
  load(false)
}

function onSearch() {
  toast('检索功能开发中')
}

function onCategory(item) {
  toast(`分类「${item.name}」筛选开发中`)
}

onLoad(() => {
  load(true)
})

onPullDownRefresh(() => {
  load(true, true)
})

onReachBottom(() => {
  loadMore()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.search-wrap {
  padding: 20rpx 32rpx 8rpx;
  background: #ffffff;
}

.search-box {
  height: 76rpx;
  border-radius: 38rpx;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  padding: 0 28rpx;
}

.search-icon {
  font-size: 34rpx;
  color: #64748b;
  margin-right: 12rpx;
}

.search-placeholder {
  font-size: 26rpx;
  color: #94a3b8;
}

.section {
  margin: 24rpx 32rpx 0;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 8rpx 30rpx rgba(15, 23, 42, 0.04);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.section-extra {
  font-size: 24rpx;
  color: #94a3b8;
}

.notice-scroll {
  width: 100%;
  white-space: nowrap;
}

.notice-row {
  display: flex;
}

.notice-card {
  width: 520rpx;
  flex-shrink: 0;
  margin-right: 20rpx;
  padding: 24rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #eef5ff, #f7fbff);
  border: 1rpx solid #dbeafe;
}

.notice-tag {
  display: inline-block;
  font-size: 20rpx;
  color: #1677ff;
  background: #dbeafe;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  margin-bottom: 12rpx;
}

.notice-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-content {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-empty {
  height: 96rpx;
  border-radius: 16rpx;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  color: #94a3b8;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16rpx;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.category-icon {
  width: 88rpx;
  height: 88rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 10rpx;
}

.icon-0 {
  background: linear-gradient(135deg, #1677ff, #6aa8ff);
}

.icon-1 {
  background: linear-gradient(135deg, #7c3aed, #a78bfa);
}

.icon-2 {
  background: linear-gradient(135deg, #f97316, #fdba74);
}

.icon-3 {
  background: linear-gradient(135deg, #14b8a6, #5eead4);
}

.icon-4 {
  background: linear-gradient(135deg, #64748b, #94a3b8);
}

.category-name {
  font-size: 24rpx;
  color: #334155;
}

.found-list {
  display: flex;
  flex-direction: column;
}

.found-card {
  display: flex;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #eef2f7;
}

.found-card:last-of-type {
  border-bottom: none;
}

.found-left {
  margin-right: 20rpx;
}

.thumb {
  width: 96rpx;
  height: 96rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #e0edff, #f2f8ff);
  color: #1677ff;
  font-size: 36rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.found-body {
  flex: 1;
  min-width: 0;
}

.found-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.found-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  flex-shrink: 0;
  margin-left: 16rpx;
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
}

.status-0 {
  color: #1677ff;
  background: #e0edff;
}

.status-1 {
  color: #f59e0b;
  background: #fef3c7;
}

.status-2,
.status-3 {
  color: #94a3b8;
  background: #f1f5f9;
}

.found-meta {
  display: block;
  font-size: 24rpx;
  color: #64748b;
  margin-top: 4rpx;
}

.state-box {
  padding: 60rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.state-title {
  font-size: 30rpx;
  color: #334155;
  font-weight: 600;
}

.state-desc {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.retry-btn {
  margin-top: 26rpx;
  height: 68rpx;
  line-height: 68rpx;
  padding: 0 40rpx;
  border-radius: 34rpx;
  background: #1677ff;
  color: #ffffff;
  font-size: 26rpx;
}

.load-more {
  padding: 28rpx 0 8rpx;
  text-align: center;
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
