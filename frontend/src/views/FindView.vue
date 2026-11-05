<template>
  <div class="page-wrapper">
    <div class="find-container">
      <h1 class="title">用户信息卡片</h1>

      <!-- 1. 加载中的状态：加一个旋转动画 -->
      <div v-if="loading" class="status-box">
        <div class="spinner"></div>
        <p>数据加载中，请稍候...</p>
      </div>

      <!-- 2. 错误状态：加个红色的警告样式 -->
      <div v-else-if="error" class="status-box error-box">
        <p>❌ 出错了：{{ error }}</p>
        <button class="retry-btn" @click="fetchUser">重新加载</button>
      </div>

      <!-- 3. 成功状态：展示用户信息 -->
      <div v-else-if="user" class="user-info">
        <div class="details">
          <div class="detail-item">
            <span class="label">用户名：</span>
            <span class="value">{{ user.name }}</span>
          </div>
          <div class="detail-item">
            <span class="label">角色：</span>
            <span class="value">{{ user.role }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "FindView",
  data() {
    return {
      user: null,
      loading: true,
      error: null,
    };
  },
  mounted() {
    this.fetchUser();
  },
  methods: {
    async fetchUser() {
      try {
        this.loading = true;
        const response = await fetch("/admin/find", {
          headers: { Authorization: localStorage.getItem("token") || "" },
        });
        if (!response.ok) {
          throw new Error("网络响应错误");
        }
        this.user = await response.json();
      } catch (err) {
        this.error = err.message;
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>

<style scoped>
/* 1. 给整个页面加一个柔和的渐变背景，并让卡片居中 */
.find-container {
  /* 限制最大宽度，并居中 */
  max-width: 450px;
  margin: 60px auto;
  padding: 40px 30px;

  /* 纯白背景 + 现代感圆角 + 柔和的阴影 */
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);

  /* 字体设置 */
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Microsoft YaHei",
    sans-serif;

  /* 如果需要铺满全屏的背景，可以给 body 设背景，这里为了简单只对卡片美化 */
  border: none;
}

/* 2. 标题美化 */
h1 {
  text-align: center;
  color: #2c3e50;
  font-size: 24px;
  margin-bottom: 30px;
  font-weight: 600;
  position: relative;
}

/* 标题下方加一个小绿条（呼应Vue的主题色） */
h1::after {
  content: "";
  display: block;
  width: 40px;
  height: 4px;
  background-color: #42b983;
  margin: 10px auto 0;
  border-radius: 2px;
}

/* 3. 数据展示区美化 */
p {
  margin: 15px 0;
  font-size: 16px;
  color: #555;
  display: flex;
  justify-content: space-between; /* 让文字和值左右对齐 */
  padding: 12px 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #42b983; /* 左侧加个强调边条 */
  transition: all 0.3s ease;
}

p:hover {
  background-color: #f1f3f5;
  transform: translateX(5px); /* 鼠标悬浮时轻微右移，增加交互感 */
}

/* 4. 加载中和错误状态的简单美化 */
.loading-text,
.error-text {
  text-align: center;
  padding: 20px;
  font-size: 16px;
}

.error-text {
  color: #e74c3c;
  background-color: #fdf0ed;
  border-radius: 8px;
}
</style>
