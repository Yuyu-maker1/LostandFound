<script setup>
import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import router from "@/router";

// 当前登录账号（管理员账号由车站统一分配，不开放自助注册）
const username = ref(localStorage.getItem("username") || "管理员");

/* ==================== 修改密码 ==================== */

const pwdForm = reactive({
  oldPwd: "",
  newPwd: "",
  confirmPwd: "",
});

const pwdFormRef = ref(null);
const submitting = ref(false);

// 显式映射，避免把未知/空角色兜底成“车站管理员”
const roleText = computed(() => {
  const role = localStorage.getItem("role");
  if (role === "super_admin") return "超级管理员";
  if (role === "admin") return "车站管理员";
  return "未知角色";
});

// 确认新密码与登录密码一致
const validateConfirm = (rule, value, callback) => {
  if (value !== pwdForm.newPwd) {
    callback(new Error("两次输入的新密码不一致"));
  } else {
    callback();
  }
};

const pwdRules = {
  oldPwd: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPwd: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度应为 6-20 个字符", trigger: "blur" },
  ],
  confirmPwd: [
    { required: true, message: "请再次输入新密码", trigger: "blur" },
    { validator: validateConfirm, trigger: "blur" },
  ],
};

// 修改密码：第一步校验旧密码，第二步提交新密码
const handleUpdatePwd = async () => {
  const valid = await pwdFormRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    /* ---------- 第一步：校验旧密码是否正确 ---------- */
    const checkRes = await fetch("/admin/password", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: localStorage.getItem("token") || "",
      },
      body: JSON.stringify({ oldPwd: pwdForm.oldPwd }),
    });
    // 拦截器返回 401 时说明登录态已失效，不能误报为"原密码错误"
    if (checkRes.status === 401) {
      ElMessage.error("登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    const oldPwdRight = await checkRes.json();

    if (oldPwdRight !== true) {
      ElMessage.error("原密码错误，请重新输入");
      submitting.value = false;
      return;
    }

    /* ---------- 第二步：提交新密码 ---------- */
    const updateRes = await fetch("/admin/userInfo", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: localStorage.getItem("token") || "",
      },
      body: JSON.stringify({
        newPwd: pwdForm.newPwd,
      }),
    });
    if (updateRes.status === 401) {
      ElMessage.error("登录已过期，请重新登录");
      router.push("/login");
      return;
    }
    const updateOk = await updateRes.json();

    if (updateOk === true) {
      ElMessage.success("密码修改成功，请重新登录");
      // 清除localStorage中的数据
      localStorage.removeItem("userName");
      localStorage.removeItem("token");
      pwdFormRef.value.resetFields();
      setTimeout(() => {
        router.push("/login");
      }, 1500);
    } else {
      ElMessage.error("密码修改失败，请稍后重试");
    }
  } catch (err) {
    console.error("修改密码请求异常:", err);
    ElMessage.error("网络异常，请确认后端服务已启动");
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <div class="account-page">
    <!-- 账号概览 -->
    <section class="card">
      <h3 class="card-title">账号信息</h3>

      <div class="meta-row">
        <span class="meta-label">登录账号</span>
        <span class="meta-value">{{ username }}</span>
      </div>
      <div class="meta-row">
        <span class="meta-label">账户角色</span>
        <span class="meta-value"
          ><span class="role-tag">{{ roleText }}</span></span
        >
      </div>
      <div class="meta-row">
        <span class="meta-label">权限范围</span>
        <span class="meta-value muted">
          用户管理、信息审核、认领审核、交接归档、公告发布、数据统计
        </span>
      </div>
    </section>

    <!-- 修改密码 -->
    <section class="card">
      <h3 class="card-title">修改密码</h3>
      <p class="card-sub">修改密码需要先校验原密码。</p>

      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="96px"
        class="pwd-form"
        @submit.prevent
      >
        <el-form-item label="原密码" prop="oldPwd">
          <el-input
            v-model="pwdForm.oldPwd"
            type="password"
            placeholder="请输入当前登录密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPwd">
          <el-input
            v-model="pwdForm.newPwd"
            type="password"
            placeholder="请输入 6-20 位新密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPwd">
          <el-input
            v-model="pwdForm.confirmPwd"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            class="submit-btn"
            @click="handleUpdatePwd"
          >
            确认修改
          </el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="tip">
      <h4>安全提示</h4>
      <p>
        管理员账号拥有全平台审核与数据维护权限，请定期更换密码，切勿与其他系统使用相同密码。
        修改成功后，本次会话立即生效，无需重新登录。
      </p>
    </section>
  </div>
</template>

<style scoped>
.account-page {
  max-width: 860px;
}

.card {
  padding: 24px 28px;
  margin-bottom: 20px;
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.card-title {
  margin: 0 0 8px;
  padding-left: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0e2f5c;
  border-left: 3px solid #14539e;
  line-height: 1.2;
}

.card-sub {
  margin: 0 0 22px;
  font-size: 12px;
  line-height: 1.7;
  color: #8a97a8;
}

.meta-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin-bottom: 14px;
  font-size: 13px;
}

.meta-label {
  flex: 0 0 70px;
  color: #8a97a8;
}

.meta-value {
  flex: 1;
  color: #26364d;
}

.meta-value.muted {
  color: #71809a;
  line-height: 1.7;
}

.role-tag {
  padding: 2px 9px;
  font-size: 12px;
  color: #14539e;
  background: #eaf1fa;
  border-radius: 3px;
}

.pwd-form {
  max-width: 460px;
}

.submit-btn {
  width: 120px;
  background: #14539e;
  border: none;
}

.submit-btn:hover {
  background: #1a68c4;
}

.tip {
  padding: 18px 22px;
  background: #fffdf5;
  border: 1px solid #f2e6c8;
  border-left: 3px solid #e0b64a;
  border-radius: 6px;
}

.tip h4 {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #8a6d1f;
}

.tip p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #7d6f52;
}
</style>
