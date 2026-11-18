<template>
  <view class="train-no-keyboard">
    <view class="train-input" @click="openKeyboard">
      <text v-if="value" class="train-value">{{ value }}</text>
      <text v-else class="train-placeholder">{{ placeholder || '输入车次' }}</text>
      <text v-if="visible" class="train-caret">|</text>
      <text class="train-arrow">›</text>
    </view>

    <view v-if="visible" class="kb-mask" @click="closeKeyboard"></view>

    <view v-if="visible" class="kb-panel">
      <view class="kb-head">
        <text class="kb-title">输入车次</text>
        <text class="kb-cancel" @click="closeKeyboard">收起</text>
      </view>

      <view v-for="(row, rowIndex) in rows" :key="rowIndex" class="kb-row">
        <view
          v-for="key in row"
          :key="key.label"
          class="kb-key"
          :class="[
            'key-' + key.type,
            { 'key-wide': key.span === 2 },
            { 'key-disabled': key.type === 'ok' && !isValid }
          ]"
          @click="press(key)"
        >
          {{ key.label }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '输入车次'
  }
})

const emit = defineEmits(['update:modelValue'])

const LETTERS = ['G', 'T', 'K', 'D', 'C', 'S', 'Z', 'N', 'V', 'Y', 'L']
const visible = ref(false)

const value = computed(() => props.modelValue || '')

const rows = [
  [
    { label: 'G', type: 'letter' },
    { label: 'T', type: 'letter' },
    { label: 'K', type: 'letter' },
    { label: 'D', type: 'letter' }
  ],
  [
    { label: 'C', type: 'letter' },
    { label: 'S', type: 'letter' },
    { label: 'Z', type: 'letter' },
    { label: 'N', type: 'letter' }
  ],
  [
    { label: '1', type: 'digit' },
    { label: '2', type: 'digit' },
    { label: '3', type: 'digit' },
    { label: 'V', type: 'letter' }
  ],
  [
    { label: '4', type: 'digit' },
    { label: '5', type: 'digit' },
    { label: '6', type: 'digit' },
    { label: 'Y', type: 'letter' }
  ],
  [
    { label: '7', type: 'digit' },
    { label: '8', type: 'digit' },
    { label: '9', type: 'digit' },
    { label: 'L', type: 'letter' }
  ],
  [
    { label: '0', type: 'digit' },
    { label: '⌫', type: 'del' },
    { label: '确认', type: 'ok', span: 2 }
  ]
]

const isValid = computed(() => /^([GDCZTKSYL]\d{1,4}|\d{2,5})$/.test(value.value))

function openKeyboard() {
  visible.value = true
}

function closeKeyboard() {
  visible.value = false
}

function press(key) {
  if (key.type === 'letter') {
    // 字母只能有一个且必须在第一位：点字母直接清空，只保留该字母
    emit('update:modelValue', key.label)
    return
  }

  if (key.type === 'del') {
    emit('update:modelValue', value.value.slice(0, -1))
    return
  }

  if (key.type === 'ok') {
    if (isValid.value) {
      visible.value = false
    }
    return
  }

  // 数字
  const current = value.value
  const hasLetter = LETTERS.includes(current.charAt(0))
  if (hasLetter) {
    const digits = current.slice(1)
    if (digits.length >= 4) return
  } else if (current.length >= 5) {
    return
  }
  emit('update:modelValue', current + key.label)
}
</script>

<style scoped>
.train-input {
  display: flex;
  align-items: center;
  min-height: 88rpx;
  padding: 0 28rpx;
  background: #f5f7fb;
  border-radius: 16rpx;
  font-size: 32rpx;
}

.train-value {
  flex: 1;
  color: #1f2937;
  letter-spacing: 2rpx;
}

.train-placeholder {
  flex: 1;
  color: #a8b3c4;
}

.train-caret {
  color: #1677ff;
  font-size: 34rpx;
  animation: blink 1s steps(1) infinite;
}

.train-arrow {
  margin-left: 12rpx;
  color: #94a3b8;
  font-size: 32rpx;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.kb-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.35);
  z-index: 900;
}

.kb-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 901;
  padding: 16rpx 12rpx 28rpx;
  background: #f1f5f9;
  border-radius: 24rpx 24rpx 0 0;
  box-shadow: 0 -8rpx 30rpx rgba(15, 23, 42, 0.12);
}

.kb-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 16rpx 20rpx;
}

.kb-title {
  font-size: 26rpx;
  color: #64748b;
}

.kb-cancel {
  font-size: 28rpx;
  color: #1677ff;
}

.kb-row {
  display: flex;
  margin-bottom: 10rpx;
}

.kb-key {
  flex: 1;
  height: 92rpx;
  margin: 0 6rpx;
  border-radius: 12rpx;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  color: #0f172a;
  box-shadow: 0 2rpx 0 rgba(15, 23, 42, 0.08);
}

.key-letter {
  color: #f59e0b;
  font-weight: 600;
}

.key-del {
  color: #475569;
}

.key-ok {
  flex: 2;
  color: #1677ff;
  font-weight: 600;
}

.key-wide {
  flex: 2;
}

.key-disabled {
  color: #b6c2d2;
}
</style>
