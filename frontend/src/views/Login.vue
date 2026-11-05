<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

const router = useRouter();

// 登录表单数据
const loginForm = reactive({
  username: localStorage.getItem("rememberName") || "",
  password: "",
  verifyCode: "",
});

// 记住账号
const rememberMe = ref(!!localStorage.getItem("rememberName"));

// 表单校验规则
const loginRules = {
  username: [{ required: true, message: "请输入登录账号", trigger: "blur" }],
  password: [
    { required: true, message: "请输入登录密码", trigger: "blur" },
    { min: 3, max: 20, message: "密码长度应为 3-20 个字符", trigger: "blur" },
  ],
  verifyCode: [{ required: true, message: "请输入验证码", trigger: "blur" }],
};

const loginFormRef = ref(null);
const loading = ref(false);

// ===== 图形验证码（Hutool 生成，存后端 Session，直接输出图片流）=====
// 验证码图片 URL，后端路径 /util/verifycode（dev 经 vue.config.js 代理到后端）
const verifyCodeUrl = ref("/util/verifycode");

// 刷新验证码：加时间戳参数，防止浏览器缓存
const refreshVerifyCode = () => {
  verifyCodeUrl.value = "/util/verifycode?" + Date.now();
  loginForm.verifyCode = "";
};

// 组件挂载时刷新验证码
onMounted(() => {
  refreshVerifyCode();
});

// 登录提交
const handleLogin = async () => {
  if (!loginFormRef.value) return;
  const valid = await loginFormRef.value.validate().catch(() => false);
  if (!valid) return;
  // 验证码为 4 位，前端先做基本拦截
  if (loginForm.verifyCode.trim().length !== 4) {
    ElMessage.warning("验证码为 4 位");
    return;
  }

  loading.value = true;
  try {
    const response = await fetch("/admin/sys_login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      // 同源请求（dev 由代理转发）默认携带 Cookie，Session 中的验证码据此校验
      credentials: "include",
      body: JSON.stringify({
        username: loginForm.username,
        password: loginForm.password,
        verifyCode: loginForm.verifyCode.trim(),
      }),
    });

    const result = await response.json();

    if (result.code === 0) {
      ElMessage.success(result.msg || "登录成功");

      const user = result.data1 || {};
      localStorage.setItem("username", user.name || loginForm.username);
      // 以登录响应中的真实角色为准，不再兜底成 admin（避免把未知角色显示成车站管理员）
      localStorage.setItem("role", user.role || "");
      localStorage.setItem("token", result.token || "");

      if (rememberMe.value) {
        localStorage.setItem("rememberName", loginForm.username);
      } else {
        localStorage.removeItem("rememberName");
      }

      // 登录成功后进入系统首页
      router.push("/index");
    } else {
      ElMessage.error(result.msg || "登录失败");
      // 验证码已一次性失效，失败后统一刷新
      refreshVerifyCode();
    }
  } catch (err) {
    console.error("登录请求异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
    refreshVerifyCode();
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <section class="brand-panel">
      <div class="brand-grid"></div>

      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" width="26" height="26" fill="none">
            <path
              d="M6 3h12a2 2 0 0 1 2 2v9a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V5a2 2 0 0 1 2-2Z"
              stroke="#fff"
              stroke-width="1.6"
              stroke-linejoin="round"
            />
            <path d="M4 9h16" stroke="#fff" stroke-width="1.6" />
            <circle cx="8.5" cy="13.5" r="1.2" fill="#fff" />
            <circle cx="15.5" cy="13.5" r="1.2" fill="#fff" />
            <path
              d="m6 17-2 4M18 17l2 4"
              stroke="#fff"
              stroke-width="1.6"
              stroke-linecap="round"
            />
          </svg>
          <span>高铁失物招领</span>
        </div>

        <h1>高铁失物招领系统</h1>
        <p class="brand-sub">
          HIGH-SPEED RAILWAY LOST &amp; FOUND MANAGEMENT PLATFORM
        </p>

        <ul class="brand-features">
          <li>
            <span class="dot"></span>
            乘务员录入 · 旅客认领 · 管理员审核，全流程线上闭环
          </li>
          <li>
            <span class="dot"></span>
            实名认证与凭证核验双重把关，杜绝恶意冒领
          </li>
          <li>
            <span class="dot"></span>
            AI 智能匹配模糊描述，提升失物找回成功率
          </li>
        </ul>
      </div>

      <div class="brand-footer">© 2026 高铁失物招领系统</div>
    </section>

    <!-- 右侧登录表单 -->
    <section class="form-panel">
      <div class="form-inner">
        <header class="form-head">
          <h2>账号登录</h2>
          <p>欢迎回来，请使用系统账号登录</p>
        </header>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          size="large"
          @submit.prevent
        >
          <el-form-item label="登录账号" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入登录账号"
              clearable
            />
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入登录密码"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <!-- 图形验证码：点击图片刷新 -->
          <el-form-item label="验证码" prop="verifyCode">
            <div class="captcha-container">
              <el-input
                v-model="loginForm.verifyCode"
                placeholder="请输入验证码"
                maxlength="4"
                class="captcha-input"
                @keyup.enter="handleLogin"
              />
              <img
                :src="verifyCodeUrl"
                class="captcha-img"
                alt="验证码"
                @click="refreshVerifyCode"
              />
            </div>
          </el-form-item>

          <div class="form-extra">
            <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
          </div>

          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f4f6fa;
}

/* ============ 左侧品牌区 ============ */
.brand-panel {
  position: relative;
  flex: 1 1 56%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 8%;
  color: #eaf2ff;
  background: linear-gradient(155deg, #071c36 0%, #0e2f5c 45%, #14539e 100%);
  overflow: hidden;
}

.brand-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(
      rgba(255, 255, 255, 0.045) 1px,
      transparent 1px
    ),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(circle at 30% 40%, #000 0%, transparent 78%);
  -webkit-mask-image: radial-gradient(
    circle at 30% 40%,
    #000 0%,
    transparent 78%
  );
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 520px;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 7px 16px 7px 12px;
  margin-bottom: 34px;
  font-size: 15px;
  letter-spacing: 1px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.brand-content h1 {
  margin: 0 0 14px;
  font-size: 36px;
  font-weight: 600;
  letter-spacing: 2px;
  line-height: 1.3;
  color: #fff;
}

.brand-sub {
  margin: 0 0 44px;
  font-size: 11px;
  letter-spacing: 2.4px;
  color: rgba(215, 231, 255, 0.55);
  line-height: 1.8;
}

.brand-features {
  list-style: none;
  margin: 0;
  padding: 0;
}

.brand-features li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(226, 238, 255, 0.78);
}

.dot {
  flex: 0 0 6px;
  width: 6px;
  height: 6px;
  margin-top: 9px;
  border-radius: 50%;
  background: #4f9dff;
  box-shadow: 0 0 0 4px rgba(79, 157, 255, 0.16);
}

.brand-footer {
  position: absolute;
  left: 8%;
  bottom: 28px;
  font-size: 12px;
  letter-spacing: 0.5px;
  color: rgba(200, 220, 245, 0.42);
}

/* ============ 右侧表单区 ============ */
.form-panel {
  flex: 0 0 44%;
  min-width: 460px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 56px;
  background: #fff;
}

.form-inner {
  width: 100%;
  max-width: 368px;
}

.form-head {
  margin-bottom: 32px;
}

.form-head h2 {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #0e2f5c;
}

.form-head p {
  margin: 0;
  font-size: 13px;
  color: #8a97a8;
}

.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 22px;
}

.link {
  font-size: 13px;
  color: #1d63c9;
  cursor: pointer;
}

.link:hover {
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  letter-spacing: 4px;
  font-weight: 500;
  border: none;
  border-radius: 6px;
  background: #14539e;
  transition: background 0.2s;
}

.login-btn:hover {
  background: #1a68c4;
}

/* 图形验证码 */
.captcha-container {
  display: flex;
  align-items: center;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  flex: none;
  width: 150px;
  height: 30px;
  margin-left: 10px;
  border: 1px solid #dce3ec;
  border-radius: 6px;
  background: #f4f7fb;
  cursor: pointer;
}

.form-tip {
  margin: 26px 0 0;
  padding: 12px 14px;
  font-size: 12px;
  line-height: 1.6;
  color: #5c6b80;
  background: #f4f7fb;
  border-left: 3px solid #c3d6ee;
  border-radius: 4px;
}

.form-footer {
  margin-top: 28px;
  font-size: 12px;
  text-align: center;
  color: #a0aab8;
}

.reg-link {
  color: #1d63c9;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s;
}

.reg-link:hover {
  color: #14539e;
  text-decoration: underline;
}

/* 表单控件细节 */
:deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #465366;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #dce3ec inset;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b6c8dd inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #14539e inset;
}

/* 响应式：窄屏隐藏左侧品牌区 */
@media screen and (max-width: 1024px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    flex: 1 1 100%;
    min-width: 0;
    padding: 32px 24px;
    background: #f4f6fa;
  }

  .form-inner {
    max-width: 380px;
    padding: 36px 28px;
    background: #fff;
    border-radius: 10px;
    box-shadow: 0 8px 32px rgba(14, 47, 92, 0.1);
  }
}
</style>
