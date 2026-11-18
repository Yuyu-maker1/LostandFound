<template>
  <view class="publish-page">
    <view class="intro">
      <text class="intro-title">拾获物品登记</text>
      <text class="intro-sub">
        {{ isAttendant ? '乘务员官方登记，提交后等待车站管理员审核' : '旅客补充登记，提交前请确认保管方式' }}
      </text>
    </view>

    <view v-if="isAttendant" class="official-tip">
      <text class="official-tag">官方认证</text>
      <text>当前为乘务员工作账号，发布信息将标注官方认证。</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">物品名称<text class="required">*</text></text>
        <input
          v-model="form.title"
          class="field-input"
          placeholder="如：黑色双肩包"
          placeholder-class="placeholder"
          maxlength="50"
        />
      </view>

      <view class="field">
        <text class="field-label">物品分类<text class="required">*</text></text>
        <picker
          mode="selector"
          :range="categoryNames"
          :value="categoryIndex < 0 ? 0 : categoryIndex"
          @change="onCategoryChange"
        >
          <view class="field-input picker-value">
            <text :class="categoryName ? 'picker-text' : 'placeholder'">
              {{ categoryName || '请选择物品分类' }}
            </text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="field">
        <text class="field-label">特征描述</text>
        <textarea
          v-model="form.description"
          class="field-textarea"
          placeholder="颜色、品牌、外观细节等"
          placeholder-class="placeholder"
          maxlength="255"
        />
      </view>

      <view class="field">
        <text class="field-label">物品照片<text class="required">*</text></text>
        <view class="image-box" @click="chooseImage">
          <image
            v-if="form.itemImage"
            :src="imageUrl"
            class="image-preview"
            mode="aspectFill"
          />
          <view v-else class="image-empty">
            <text class="image-plus">+</text>
            <text class="image-tip">上传照片</text>
          </view>
        </view>
      </view>

      <view class="field">
        <text class="field-label">车次<text class="required">*</text></text>
        <TrainNoKeyboard v-model="form.trainNo" placeholder="输入车次，如 G1234" />
        <text class="field-hint">第一位可为字母或数字；字母后最多 4 位数字，纯数字最多 5 位</text>
      </view>

      <view class="field-row">
        <view class="field half">
          <text class="field-label">车厢号</text>
          <input
            v-model="form.carriageNo"
            class="field-input"
            placeholder="如 3"
            placeholder-class="placeholder"
            maxlength="10"
          />
        </view>
        <view class="field half">
          <text class="field-label">座位号</text>
          <input
            v-model="form.seatNo"
            class="field-input"
            placeholder="如 12A"
            placeholder-class="placeholder"
            maxlength="10"
          />
        </view>
      </view>

      <view class="field-row">
        <view class="field half">
          <text class="field-label">拾获日期<text class="required">*</text></text>
          <picker mode="date" :value="form.foundDate" @change="onDateChange">
            <view class="field-input picker-value">
              <text class="picker-text">{{ form.foundDate }}</text>
              <text class="picker-arrow">›</text>
            </view>
          </picker>
        </view>
        <view class="field half">
          <text class="field-label">拾获时间<text class="required">*</text></text>
          <picker mode="time" :value="form.foundClock" @change="onTimeChange">
            <view class="field-input picker-value">
              <text class="picker-text">{{ form.foundClock }}</text>
              <text class="picker-arrow">›</text>
            </view>
          </picker>
        </view>
      </view>

      <view class="field">
        <text class="field-label">拾获地点<text class="required">*</text></text>
        <input
          v-model="form.foundLocation"
          class="field-input"
          placeholder="如：3号车厢12A座位"
          placeholder-class="placeholder"
          maxlength="50"
        />
      </view>

      <view class="field">
        <text class="field-label">保管方式<text class="required">*</text></text>
        <view v-if="isAttendant" class="field-input fixed-value">交服务台</view>
        <view v-else class="custody-group">
          <view
            class="custody-item"
            :class="{ active: form.custody === 0 }"
            @click="form.custody = 0"
          >
            交服务台
          </view>
          <view
            class="custody-item"
            :class="{ active: form.custody === 1 }"
            @click="form.custody = 1"
          >
            自行保管
          </view>
        </view>
      </view>

      <view v-if="!isAttendant && form.custody === 0" class="field">
        <text class="field-label">移交车站<text class="required">*</text></text>
        <picker
          mode="selector"
          :range="stationNames"
          :value="stationIndex < 0 ? 0 : stationIndex"
          @change="onStationChange"
        >
          <view class="field-input picker-value">
            <text :class="stationName ? 'picker-text' : 'placeholder'">
              {{ stationName || '请选择移交车站' }}
            </text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="field">
        <text class="field-label">保管地点<text class="required">*</text></text>
        <input
          v-if="isAttendant"
          v-model="form.storageLocation"
          class="field-input"
          placeholder="如：上海虹桥站服务台"
          placeholder-class="placeholder"
          maxlength="50"
        />
        <view v-else class="field-input fixed-value">
          {{ form.custody === 1 ? '发布者自行保管' : storagePreview }}
        </view>
      </view>

      <button class="submit-btn" :loading="submitting" :disabled="submitting" @click="onSubmit">
        {{ submitting ? '提交中...' : '提交审核' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { BASE_URL, get, post, uploadFile } from '../../utils/request'
import TrainNoKeyboard from '../../components/TrainNoKeyboard.vue'

const userInfo = ref(uni.getStorageSync('userInfo') || {})
const isAttendant = computed(() => userInfo.value.role === 'attendant')

const categories = ref([])
const stations = ref([])
const categoryIndex = ref(-1)
const stationIndex = ref(-1)
const submitting = ref(false)
const uploading = ref(false)

const today = new Date()
const pad = (n) => String(n).padStart(2, '0')
const defaultDate = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
const defaultClock = `${pad(today.getHours())}:${pad(today.getMinutes())}`

const form = reactive({
  title: '',
  categoryId: null,
  description: '',
  itemImage: '',
  trainNo: '',
  carriageNo: '',
  seatNo: '',
  foundDate: defaultDate,
  foundClock: defaultClock,
  foundLocation: '',
  custody: isAttendant.value ? 0 : 1,
  stationId: null,
  storageLocation: ''
})

const categoryNames = computed(() => categories.value.map((item) => item.categoryName))
const categoryName = computed(() => {
  const item = categories.value[categoryIndex.value]
  return item ? item.categoryName : ''
})

const stationNames = computed(() => stations.value.map((item) => item.stationName))
const stationName = computed(() => {
  const item = stations.value[stationIndex.value]
  return item ? item.stationName : ''
})

const storagePreview = computed(() =>
  stationName.value ? `${stationName.value}服务台` : '请先选择移交车站'
)

const imageUrl = computed(() => {
  if (!form.itemImage) return ''
  return form.itemImage.startsWith('http') ? form.itemImage : BASE_URL + form.itemImage
})

async function loadBaseData() {
  try {
    const categoryRes = await get('/app/categories')
    categories.value = categoryRes.data || []
  } catch (e) {
    uni.showToast({ title: e.message || '分类加载失败', icon: 'none' })
  }

  if (!isAttendant.value) {
    try {
      const stationRes = await get('/app/stations')
      stations.value = stationRes.data || []
    } catch (e) {
      uni.showToast({ title: e.message || '车站加载失败', icon: 'none' })
    }
  }
}

function onCategoryChange(e) {
  categoryIndex.value = Number(e.detail.value)
  const item = categories.value[categoryIndex.value]
  form.categoryId = item ? item.categoryId : null
}

function onStationChange(e) {
  stationIndex.value = Number(e.detail.value)
  const item = stations.value[stationIndex.value]
  form.stationId = item ? item.id : null
}

function onDateChange(e) {
  form.foundDate = e.detail.value
}

function onTimeChange(e) {
  form.foundClock = e.detail.value
}

function chooseImage() {
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
        const data = await uploadFile('/app/found/image', filePath)
        form.itemImage = data.data1 || ''
        uni.showToast({ title: '照片已上传', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: e.message || '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
        uploading.value = false
      }
    }
  })
}

function validate() {
  if (!form.title.trim()) {
    return '请输入物品名称'
  }
  if (!form.categoryId) {
    return '请选择物品分类'
  }
  if (!form.itemImage) {
    return '请上传物品照片'
  }
  if (!/^([GDCZTKSYL]\d{1,4}|\d{2,5})$/.test(form.trainNo)) {
    return '车次格式不正确'
  }
  if (!form.foundDate || !form.foundClock) {
    return '请选择拾获时间'
  }
  const foundTime = new Date(`${form.foundDate}T${form.foundClock}:00`)
  if (foundTime.getTime() > Date.now()) {
    return '拾获时间不能晚于当前时间'
  }
  if (!form.foundLocation.trim()) {
    return '请输入拾获地点'
  }
  if (isAttendant.value && !form.storageLocation.trim()) {
    return '请填写保管地点'
  }
  if (!isAttendant.value && form.custody === 0 && !form.stationId) {
    return '请选择移交车站'
  }
  return ''
}

function onSubmit() {
  const error = validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }

  if (isAttendant.value) {
    doSubmit()
    return
  }

  uni.showModal({
    title: '温馨提示',
    content:
      '为了确保物品安全与规范交接，建议您将拾获物品移交给最近的乘务员或车站服务台。若您选择自行保管，请务必对物品安全负责。',
    confirmText: '确认提交',
    cancelText: '再想想',
    success: (res) => {
      if (res.confirm) {
        doSubmit()
      }
    }
  })
}

async function doSubmit() {
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      title: form.title.trim(),
      categoryId: form.categoryId,
      description: form.description.trim(),
      itemImage: form.itemImage,
      trainNo: form.trainNo,
      carriageNo: form.carriageNo.trim(),
      seatNo: form.seatNo.trim(),
      foundTime: `${form.foundDate} ${form.foundClock}:00`,
      foundLocation: form.foundLocation.trim(),
      custody: isAttendant.value ? 0 : form.custody,
      stationId: isAttendant.value ? null : form.stationId,
      storageLocation: isAttendant.value
        ? form.storageLocation.trim()
        : form.custody === 1
          ? '发布者自行保管'
          : storagePreview.value
    }
    await post('/app/found', payload)
    uni.showToast({ title: '提交成功，等待审核', icon: 'none', duration: 1500 })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/mine/publish?type=found' })
    }, 1200)
  } catch (e) {
    uni.showToast({ title: e.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad(() => {
  loadBaseData()
})
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.intro {
  padding: 36rpx 32rpx 24rpx;
  background: linear-gradient(135deg, #1677ff, #4f9bff);
  color: #ffffff;
}

.intro-title {
  display: block;
  font-size: 40rpx;
  font-weight: 600;
}

.intro-sub {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.86);
}

.official-tip {
  display: flex;
  align-items: center;
  margin: 20rpx 32rpx 0;
  padding: 18rpx 22rpx;
  background: #f0fdf4;
  border: 1rpx solid #bbf7d0;
  border-radius: 14rpx;
  font-size: 24rpx;
  color: #166534;
}

.official-tag {
  margin-right: 12rpx;
  padding: 2rpx 10rpx;
  background: #16a34a;
  color: #ffffff;
  border-radius: 6rpx;
  font-size: 20rpx;
}

.form-card {
  margin: 24rpx 32rpx 0;
  padding: 32rpx 28rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 8rpx 30rpx rgba(15, 23, 42, 0.04);
}

.field {
  margin-bottom: 26rpx;
}

.field-row {
  display: flex;
  gap: 20rpx;
}

.field.half {
  flex: 1;
  min-width: 0;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: #475569;
}

.required {
  margin-left: 4rpx;
  color: #e5484d;
}

.field-input {
  width: 100%;
  min-height: 88rpx;
  padding: 0 28rpx;
  background: #f5f7fb;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #1f2937;
  display: flex;
  align-items: center;
  box-sizing: border-box;
}

.field-textarea {
  width: 100%;
  height: 160rpx;
  padding: 20rpx 28rpx;
  background: #f5f7fb;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #1f2937;
  box-sizing: border-box;
}

.placeholder {
  color: #a8b3c4;
}

.picker-value {
  justify-content: space-between;
}

.picker-text {
  color: #1f2937;
}

.picker-arrow {
  color: #94a3b8;
  font-size: 30rpx;
}

.fixed-value {
  color: #475569;
  background: #eef2f7;
}

.field-hint {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.custody-group {
  display: flex;
  gap: 20rpx;
}

.custody-item {
  flex: 1;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fb;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #475569;
}

.custody-item.active {
  background: #eaf2ff;
  border-color: #1677ff;
  color: #1677ff;
  font-weight: 600;
}

.image-box {
  width: 200rpx;
  height: 200rpx;
  border-radius: 18rpx;
  overflow: hidden;
  background: #f5f7fb;
}

.image-preview {
  width: 200rpx;
  height: 200rpx;
}

.image-empty {
  width: 200rpx;
  height: 200rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

.image-plus {
  font-size: 52rpx;
  line-height: 1;
}

.image-tip {
  margin-top: 10rpx;
  font-size: 22rpx;
}

.submit-btn {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  margin-top: 10rpx;
  border-radius: 45rpx;
  background: linear-gradient(90deg, #1677ff, #4f9bff);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}

.submit-btn[disabled] {
  opacity: 0.7;
}
</style>
