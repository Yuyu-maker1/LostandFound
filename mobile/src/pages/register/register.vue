<template>
  <view class="register-page">
    <view class="intro">
      <text class="intro-title">创建旅客账号</text>
      <text class="intro-sub">注册后可查看招领信息、发布寻物启事</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="field-label">账号<text class="required">*</text></text>
        <input
          v-model="form.name"
          class="field-input"
          :class="{ 'input-error': errors.name }"
          type="text"
          placeholder="3-20 位字母、数字或下划线"
          placeholder-class="placeholder"
          maxlength="20"
          @input="errors.name = ''"
        />
        <text v-if="errors.name" class="error-text">{{ errors.name }}</text>
      </view>

      <view class="field">
        <text class="field-label">密码<text class="required">*</text></text>
        <view class="pwd-row" :class="{ 'input-error': errors.pwd }">
          <input
            v-model="form.pwd"
            class="field-input pwd-input"
            :password="!pwdVisible"
            placeholder="6-20 位，需含字母和数字"
            placeholder-class="placeholder"
            maxlength="20"
            @input="errors.pwd = ''"
          />
          <text class="pwd-toggle" @click="pwdVisible = !pwdVisible">
            {{ pwdVisible ? '隐藏' : '显示' }}
          </text>
        </view>
        <text v-if="errors.pwd" class="error-text">{{ errors.pwd }}</text>
      </view>

      <view class="field">
        <text class="field-label">手机号<text class="required">*</text></text>
        <input
          v-model="form.phone"
          class="field-input"
          :class="{ 'input-error': errors.phone }"
          type="number"
          placeholder="请输入 11 位手机号"
          placeholder-class="placeholder"
          maxlength="11"
          @input="errors.phone = ''"
        />
        <text v-if="errors.phone" class="error-text">{{ errors.phone }}</text>
      </view>

      <view class="field">
        <text class="field-label">真实姓名<text class="optional">（选填）</text></text>
        <input
          v-model="form.realName"
          class="field-input"
          :class="{ 'input-error': errors.realName }"
          type="text"
          placeholder="2-20 个字，用于认领核验"
          placeholder-class="placeholder"
          maxlength="20"
          @input="errors.realName = ''"
        />
        <text v-if="errors.realName" class="error-text">{{ errors.realName }}</text>
      </view>

      <button class="submit-btn" :loading="loading" :disabled="loading" @click="onSubmit">
        {{ loading ? '注册中...' : '注 册' }}
      </button>

      <view class="tips">
        <text>注册即表示同意车站失物招领相关规定</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { post } from '../../utils/request'

const form = reactive({
  name: '',
  pwd: '',
  phone: '',
  realName: ''
})

const errors = reactive({
  name: '',
  pwd: '',
  phone: '',
  realName: ''
})

const pwdVisible = ref(false)
const loading = ref(false)

function validate() {
  errors.name = ''
  errors.pwd = ''
  errors.phone = ''
  errors.realName = ''

  let valid = true
  const name = form.name.trim()
  const phone = form.phone.trim()
  const realName = form.realName.trim()

  if (!name) {
    errors.name = '请输入账号'
    valid = false
  } else if (!/^[A-Za-z0-9_]{3,20}$/.test(name)) {
    errors.name = '账号需为 3-20 位字母、数字或下划线'
    valid = false
  }

  if (!form.pwd) {
    errors.pwd = '请输入密码'
    valid = false
  } else if (form.pwd.length < 6 || form.pwd.length > 20) {
    errors.pwd = '密码长度需为 6-20 位'
    valid = false
  } else if (!/[A-Za-z]/.test(form.pwd) || !/\d/.test(form.pwd)) {
    errors.pwd = '密码需同时包含字母和数字'
    valid = false
  }

  if (!phone) {
    errors.phone = '请输入手机号'
    valid = false
  } else if (!/^1[3-9]\d{9}$/.test(phone)) {
    errors.phone = '手机号格式不正确'
    valid = false
  }

  if (realName && (realName.length < 2 || realName.length > 20)) {
    errors.realName = '真实姓名需为 2-20 个字'
    valid = false
  }

  return valid
}

async function onSubmit() {
  if (loading.value) return
  if (!validate()) return

  loading.value = true
  try {
    await post('/app/register', {
      name: form.name.trim(),
      pwd: form.pwd,
      phone: form.phone.trim(),
      realName: form.realName.trim()
    })
    // 不自动登录：注册成功后回到登录页，并把账号回填，密码由用户自己输入
    uni.setStorageSync('pendingLoginName', form.name.trim())
    uni.showToast({ title: '注册成功，请登录', icon: 'none', duration: 1500 })
    setTimeout(() => {
      uni.navigateBack({
        fail: () => uni.reLaunch({ url: '/pages/login/login' })
      })
    }, 1500)
  } catch (e) {
    // 后端错误就近展示到对应字段，其余错误统一提示
    const msg = e.message || '注册失败，请稍后重试'
    if (/账号/.test(msg)) {
      errors.name = msg
    } else if (/手机号/.test(msg)) {
      errors.phone = msg
    } else if (/密码/.test(msg)) {
      errors.pwd = msg
    } else if (/姓名/.test(msg)) {
      errors.realName = msg
    } else {
      uni.showToast({ title: msg, icon: 'none' })
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  padding: 48rpx 48rpx 60rpx;
  background: #f5f7fb;
}

.intro {
  display: flex;
  flex-direction: column;
  margin-bottom: 40rpx;
  padding: 0 8rpx;
}

.intro-title {
  font-size: 42rpx;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: 2rpx;
}

.intro-sub {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #64748b;
}

.form-card {
  background: #ffffff;
  border-radius: 28rpx;
  padding: 48rpx 40rpx 32rpx;
  box-shadow: 0 16rpx 48rpx rgba(15, 23, 42, 0.08);
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

.required {
  margin-left: 6rpx;
  color: #e5484d;
}

.optional {
  margin-left: 6rpx;
  font-size: 22rpx;
  color: #94a3b8;
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

.input-error {
  background: #fff5f5;
  border: 1rpx solid #f3b4b6;
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
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #e5484d;
}

.submit-btn {
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

.submit-btn[disabled] {
  opacity: 0.7;
}

.tips {
  margin-top: 26rpx;
  text-align: center;
  font-size: 22rpx;
  color: #94a3b8;
}
</style>
