<template>
  <view class="publish-page">
    <view class="intro">
      <text class="intro-title">遗失物品登记</text>
      <text class="intro-sub">填写乘车行程和物品特征，提交后等待管理员审核</text>
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

      <view class="field">
        <text class="field-label">乘车日期<text class="required">*</text></text>
        <picker mode="date" :value="form.travelDate" @change="onTravelDateChange">
          <view class="field-input picker-value">
            <text class="picker-text">{{ form.travelDate }}</text>
            <text class="picker-arrow">›</text>
          </view>
        </picker>
      </view>

      <view class="field-row">
        <view class="field half">
          <text class="field-label">丢失日期<text class="required">*</text></text>
          <picker mode="date" :value="form.lostDate" @change="onLostDateChange">
            <view class="field-input picker-value">
              <text class="picker-text">{{ form.lostDate }}</text>
              <text class="picker-arrow">›</text>
            </view>
          </picker>
        </view>
        <view class="field half">
          <text class="field-label">丢失时间<text class="required">*</text></text>
          <picker mode="time" :value="form.lostClock" @change="onLostTimeChange">
            <view class="field-input picker-value">
              <text class="picker-text">{{ form.lostClock }}</text>
              <text class="picker-arrow">›</text>
            </view>
          </picker>
        </view>
      </view>

      <view class="field">
        <text class="field-label">丢失地点<text class="required">*</text></text>
        <input
          v-model="form.lostLocation"
          class="field-input"
          placeholder="如：3号车厢12A座位"
          placeholder-class="placeholder"
          maxlength="50"
        />
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

const categories = ref([])
const categoryIndex = ref(-1)
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
  travelDate: defaultDate,
  lostDate: defaultDate,
  lostClock: defaultClock,
  lostLocation: ''
})

const categoryNames = computed(() => categories.value.map((item) => item.categoryName))
const categoryName = computed(() => {
  const item = categories.value[categoryIndex.value]
  return item ? item.categoryName : ''
})

const imageUrl = computed(() => {
  if (!form.itemImage) return ''
  return form.itemImage.startsWith('http') ? form.itemImage : BASE_URL + form.itemImage
})

async function loadCategories() {
  try {
    const res = await get('/app/categories')
    categories.value = res.data || []
  } catch (e) {
    uni.showToast({ title: e.message || '分类加载失败', icon: 'none' })
  }
}

function onCategoryChange(e) {
  categoryIndex.value = Number(e.detail.value)
  const item = categories.value[categoryIndex.value]
  form.categoryId = item ? item.categoryId : null
}

function onTravelDateChange(e) {
  form.travelDate = e.detail.value
}

function onLostDateChange(e) {
  form.lostDate = e.detail.value
}

function onLostTimeChange(e) {
  form.lostClock = e.detail.value
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
        const data = await uploadFile('/app/item/image', filePath)
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
  if (!form.travelDate) {
    return '请选择乘车日期'
  }
  if (form.travelDate > defaultDate) {
    return '乘车日期不能晚于今天'
  }
  if (!form.lostDate || !form.lostClock) {
    return '请选择丢失时间'
  }
  const lostTime = new Date(`${form.lostDate}T${form.lostClock}:00`)
  if (lostTime.getTime() > Date.now()) {
    return '丢失时间不能晚于当前时间'
  }
  if (!form.lostLocation.trim()) {
    return '请输入丢失地点'
  }
  return ''
}

async function onSubmit() {
  const error = validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await post('/app/lost', {
      title: form.title.trim(),
      categoryId: form.categoryId,
      description: form.description.trim(),
      itemImage: form.itemImage,
      trainNo: form.trainNo,
      carriageNo: form.carriageNo.trim(),
      seatNo: form.seatNo.trim(),
      travelDate: form.travelDate,
      lostTime: `${form.lostDate} ${form.lostClock}:00`,
      lostLocation: form.lostLocation.trim()
    })
    uni.showToast({ title: '提交成功，等待审核', icon: 'none', duration: 1500 })
    setTimeout(() => {
      uni.navigateTo({ url: '/pages/mine/publish?type=lost' })
    }, 1200)
  } catch (e) {
    uni.showToast({ title: e.message || '提交失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad(() => {
  loadCategories()
})
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.intro {
  padding: 36rpx 32rpx 24rpx;
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
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
  color: rgba(255, 255, 255, 0.9);
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

.field-hint {
  display: block;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #94a3b8;
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
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
}

.submit-btn[disabled] {
  opacity: 0.7;
}
</style>
