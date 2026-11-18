<template>
  <view class="my-publish-page">
    <view v-if="!isAttendant" class="type-bar">
      <view
        v-for="tab in typeTabs"
        :key="tab.value"
        class="type-tab"
        :class="{ active: currentType === tab.value }"
        @click="switchType(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜物品名称或车次"
        placeholder-class="search-placeholder"
        confirm-type="search"
        @confirm="onSearch"
      />
      <text v-if="keyword" class="search-clear" @click="onClearSearch">×</text>
    </view>

    <view class="filter-bar">
      <view
        v-for="tab in auditTabs"
        :key="tab.value"
        class="filter-tab"
        :class="{ active: filterAudit === tab.value }"
        @click="switchAudit(tab.value)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view v-if="!items.length && !loading" class="empty-box">
      <text class="empty-title">暂无发布记录</text>
      <text class="empty-desc">{{ emptyDesc }}</text>
      <button class="empty-btn" @click="goPublish">去登记</button>
    </view>

    <view v-else class="list">
      <view v-for="item in items" :key="item[idKey]" class="item-card">
        <view class="item-top">
          <image
            v-if="item.itemImage"
            :src="imageUrl(item.itemImage)"
            class="item-img"
            mode="aspectFill"
          />
          <view v-else class="item-img placeholder-img">无图</view>

          <view class="item-info">
            <view class="item-title-row">
              <text class="item-title">{{ item.title }}</text>
              <text class="audit-tag" :class="'audit-' + item.auditStatus">
                {{ auditText(item.auditStatus) }}
              </text>
            </view>
            <text class="item-meta">车次：{{ item.trainNo }} · {{ item.categoryName || '未分类' }}</text>

            <template v-if="currentType === 'found'">
              <text class="item-meta">拾获：{{ item.foundTime }}</text>
              <text class="item-meta">保管：{{ custodyText(item.custody) }} · {{ item.storageLocation }}</text>
            </template>
            <template v-else>
              <text class="item-meta">乘车日期：{{ item.travelDate }}</text>
              <text class="item-meta">丢失：{{ item.lostTime }} · {{ item.lostLocation }}</text>
            </template>

            <text class="item-meta">状态：{{ statusText(item.status) }}</text>
          </view>
        </view>

        <view v-if="item.auditStatus === 2 && item.auditRemark" class="reject-box">
          <text>驳回原因：{{ item.auditRemark }}</text>
        </view>

        <view class="item-actions">
          <button
            v-if="item.auditStatus === 2 && canResubmit(item)"
            class="action-btn primary"
            size="mini"
            @click="onResubmit(item)"
          >
            重新提交
          </button>
          <button
            v-if="currentType === 'found' && (item.status === 0 || item.status === 1)"
            class="action-btn danger"
            size="mini"
            @click="onOff(item)"
          >
            下架
          </button>
          <button
            v-if="currentType === 'lost' && item.auditStatus === 1 && item.status === 0"
            class="action-btn primary"
            size="mini"
            @click="onMarkFound(item)"
          >
            标记已找到
          </button>
        </view>
      </view>

      <view class="load-more">
        <text v-if="loading">加载中...</text>
        <text v-else-if="items.length >= total">已经到底啦</text>
        <text v-else @click="loadMore">上拉加载更多</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow, onReachBottom } from '@dcloudio/uni-app'
import { BASE_URL, get, post } from '../../utils/request'

const typeTabs = [
  { label: '招领信息', value: 'found' },
  { label: '寻物启事', value: 'lost' }
]

const auditTabs = [
  { label: '全部', value: '' },
  { label: '待审核', value: '0' },
  { label: '已通过', value: '1' },
  { label: '已驳回', value: '2' }
]

const userInfo = ref(uni.getStorageSync('userInfo') || {})
const isAttendant = computed(() => userInfo.value.role === 'attendant')

const currentType = ref('found')
const items = ref([])
const total = ref(0)
const page = ref(1)
const limit = 10
const loading = ref(false)
const filterAudit = ref('')
// 模糊查询关键字：物品名称 / 车次
const keyword = ref('')

const idKey = computed(() => (currentType.value === 'found' ? 'foundId' : 'lostId'))
const endpoint = computed(() =>
  currentType.value === 'found' ? '/app/found/mine' : '/app/lost/mine'
)
const emptyDesc = computed(() =>
  currentType.value === 'found'
    ? '拾获物品后可在“发布”页登记'
    : '遗失物品后可在“发布”页登记'
)

const imageUrl = (path) => {
  if (!path) return ''
  return path.startsWith('http') ? path : BASE_URL + path
}

const auditText = (status) => {
  if (status === 0) return '待审核'
  if (status === 1) return '已通过'
  if (status === 2) return '已驳回'
  return ''
}

const statusText = (status) => {
  if (currentType.value === 'lost') {
    if (status === 0) return '发布中'
    if (status === 1) return '已找到'
    return ''
  }
  if (status === 0) return '待认领'
  if (status === 1) return '认领审核中'
  if (status === 2) return '已交接'
  if (status === 3) return '已下架'
  return ''
}

const custodyText = (custody) => (custody === 1 ? '自行保管' : '交服务台')

const canResubmit = (item) =>
  currentType.value === 'found' ? item.status !== 3 : item.status !== 1

async function load(reset = false) {
  if (loading.value) return
  loading.value = true
  const targetPage = reset ? 1 : page.value + 1
  try {
    const params = { page: targetPage, limit, keyword: keyword.value.trim() }
    if (filterAudit.value !== '') {
      params.auditStatus = filterAudit.value
    }
    const res = await get(endpoint.value, params)
    const list = res.data || []
    items.value = reset ? list : items.value.concat(list)
    total.value = Number(res.count || 0)
    page.value = targetPage
  } catch (e) {
    uni.showToast({ title: e.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function resetAndLoad() {
  items.value = []
  total.value = 0
  page.value = 1
  load(true)
}

function switchType(value) {
  if (currentType.value === value) return
  currentType.value = value
  filterAudit.value = ''
  keyword.value = ''
  resetAndLoad()
}

// 关键字查询：从第一页重新查询
function onSearch() {
  resetAndLoad()
}

function onClearSearch() {
  keyword.value = ''
  resetAndLoad()
}

function switchAudit(value) {
  if (filterAudit.value === value) return
  filterAudit.value = value
  resetAndLoad()
}

function loadMore() {
  if (items.value.length >= total.value) return
  load(false)
}

function goPublish() {
  if (currentType.value === 'lost') {
    uni.navigateTo({ url: '/pages/publish/lost' })
    return
  }
  uni.navigateTo({ url: '/pages/publish/found' })
}

function onOff(item) {
  uni.showModal({
    title: '确认下架',
    content: `下架后「${item.title}」将不再展示，是否继续？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await post(`/app/found/${item.foundId}/off`)
        uni.showToast({ title: '已下架', icon: 'success' })
        resetAndLoad()
      } catch (e) {
        uni.showToast({ title: e.message || '下架失败', icon: 'none' })
      }
    }
  })
}

function onResubmit(item) {
  uni.showModal({
    title: '重新提交',
    content: '重新提交后将进入待审核状态，是否继续？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const path =
          currentType.value === 'found'
            ? `/app/found/${item.foundId}/resubmit`
            : `/app/lost/${item.lostId}/resubmit`
        await post(path)
        uni.showToast({ title: '已重新提交', icon: 'success' })
        resetAndLoad()
      } catch (e) {
        uni.showToast({ title: e.message || '提交失败', icon: 'none' })
      }
    }
  })
}

function onMarkFound(item) {
  uni.showModal({
    title: '标记已找到',
    content: `确认「${item.title}」已经找回吗？`,
    success: async (res) => {
      if (!res.confirm) return
      try {
        await post(`/app/lost/${item.lostId}/found`)
        uni.showToast({ title: '已标记为找到', icon: 'success' })
        resetAndLoad()
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    }
  })
}

onLoad((options) => {
  if (options && options.type === 'lost' && !isAttendant.value) {
    currentType.value = 'lost'
  }
})

onShow(() => {
  resetAndLoad()
})

onReachBottom(() => {
  loadMore()
})
</script>

<style scoped>
.my-publish-page {
  min-height: 100vh;
  padding: 24rpx 32rpx 40rpx;
}

.type-bar {
  display: flex;
  padding: 8rpx;
  margin-bottom: 20rpx;
  background: #ffffff;
  border-radius: 18rpx;
}

.type-tab {
  flex: 1;
  height: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  font-size: 27rpx;
  color: #64748b;
}

.type-tab.active {
  background: #eaf2ff;
  color: #1677ff;
  font-weight: 600;
}

.filter-bar {
  display: flex;
  gap: 14rpx;
  margin-bottom: 22rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  height: 76rpx;
  margin-bottom: 20rpx;
  padding: 0 26rpx;
  background: #ffffff;
  border-radius: 38rpx;
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  color: #1f2937;
}

.search-placeholder {
  color: #94a3b8;
}

.search-clear {
  padding: 0 6rpx;
  font-size: 32rpx;
  color: #94a3b8;
}

.filter-tab {
  flex: 1;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  border-radius: 32rpx;
  font-size: 24rpx;
  color: #64748b;
}

.filter-tab.active {
  background: #1677ff;
  color: #ffffff;
  font-weight: 600;
}

.item-card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background: #ffffff;
  border-radius: 22rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.04);
}

.item-top {
  display: flex;
}

.item-img {
  width: 150rpx;
  height: 150rpx;
  border-radius: 16rpx;
  margin-right: 20rpx;
  background: #f1f5f9;
  flex-shrink: 0;
}

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #94a3b8;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.item-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-tag {
  flex-shrink: 0;
  margin-left: 12rpx;
  padding: 4rpx 12rpx;
  border-radius: 18rpx;
  font-size: 20rpx;
}

.audit-0 {
  color: #b45309;
  background: #fef3c7;
}

.audit-1 {
  color: #15803d;
  background: #dcfce7;
}

.audit-2 {
  color: #b91c1c;
  background: #fee2e2;
}

.item-meta {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #64748b;
}

.reject-box {
  margin-top: 16rpx;
  padding: 14rpx 18rpx;
  background: #fef2f2;
  border-radius: 12rpx;
  font-size: 23rpx;
  color: #b91c1c;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 18rpx;
}

.action-btn {
  margin: 0;
  padding: 0 26rpx;
  height: 58rpx;
  line-height: 58rpx;
  border-radius: 29rpx;
  font-size: 24rpx;
}

.action-btn.primary {
  background: #1677ff;
  color: #ffffff;
}

.action-btn.danger {
  background: #ffffff;
  color: #e5484d;
  border: 1rpx solid #fecaca;
}

.empty-box {
  padding: 160rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #334155;
}

.empty-desc {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.empty-btn {
  margin-top: 30rpx;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 48rpx;
  border-radius: 36rpx;
  background: #1677ff;
  color: #ffffff;
  font-size: 26rpx;
}

.load-more {
  padding: 24rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
