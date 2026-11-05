<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

const router = useRouter();

const username = computed(() => localStorage.getItem("username") || "管理员");

// 角色文案：按 localStorage 中的实际角色映射，未知角色不再误显示为“车站管理员”
const roleText = computed(() => {
  const role = localStorage.getItem("role");
  if (role === "super_admin") return "超级管理员";
  if (role === "admin") return "车站管理员";
  return "未知角色";
});

// 根据当前时间生成问候语
const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return "凌晨好";
  if (hour < 12) return "上午好";
  if (hour < 14) return "中午好";
  if (hour < 18) return "下午好";
  return "晚上好";
});

// 当前日期
const today = computed(() => {
  const d = new Date();
  const week = ["日", "一", "二", "三", "四", "五", "六"][d.getDay()];
  return `${d.getFullYear()} 年 ${
    d.getMonth() + 1
  } 月 ${d.getDate()} 日  星期${week}`;
});

// 待办总量
const todoTotal = computed(() =>
  todos.reduce((sum, item) => sum + item.count, 0)
);

/* -------------------- 以下为演示数据 --------------------
   数据统计接口（数据概览 / 认领状态分布）开发完成后，
   改为从后端获取并替换这里即可，模板无需改动。
--------------------------------------------------------- */

// 数据概览（对应论文"数据统计"需求）
const stats = [
  {
    key: "claim",
    label: "待审核认领申请",
    value: 12,
    unit: "条",
    hint: "需交叉比对实名信息与乘车凭证",
    accent: true,
  },
  {
    key: "found",
    label: "待审核招领信息",
    value: 8,
    unit: "条",
    hint: "乘务员录入，审核通过后前台可见",
  },
  {
    key: "lost",
    label: "待审核寻物启事",
    value: 5,
    unit: "条",
    hint: "需核对描述完整性与行程信息",
  },
  {
    key: "handover",
    label: "本月完成交接",
    value: 46,
    unit: "件",
    hint: "认领成功率 78.4%",
  },
];

// 待办事项
const todos = [
  {
    title: "认领申请待审核",
    desc: "旅客提交的认领凭证需核实，其中 12 条等待处理",
    count: 12,
    level: "urgent",
    path: "/audit/claim",
  },
  {
    title: "招领信息待审核",
    desc: "乘务员录入的拾获物品信息，审核通过后对旅客开放检索",
    count: 8,
    level: "normal",
    path: "/audit/found",
  },
  {
    title: "寻物启事待审核",
    desc: "旅客发布的寻物信息，需过滤描述不清与重复内容",
    count: 5,
    level: "normal",
    path: "/audit/lost",
  },
];

// 快捷操作
const shortcuts = [
  { title: "审核认领申请", desc: "核心业务·防冒领", path: "/audit/claim" },
  { title: "发布系统公告", desc: "前台首页展示", path: "/notice" },
  { title: "查看数据统计", desc: "车站/车次失物分布", path: "/statistics" },
  { title: "交接记录归档", desc: "已交接清单与统计", path: "/handover" },
];

// 认领状态分布（对应论文"认领状态"四个阶段）
const claimStatus = [
  { label: "待审核", value: 12, color: "#e0a03c" },
  { label: "待交接", value: 9, color: "#1d63c9" },
  { label: "已交接", value: 46, color: "#3fa06a" },
  { label: "已驳回", value: 4, color: "#c95d5d" },
];

const claimTotal = computed(() =>
  claimStatus.reduce((sum, item) => sum + item.value, 0)
);

// 最近操作日志（对应论文"操作日志"需求）
const logs = [
  {
    time: "11-02 10:24",
    action: "审核通过认领申请",
    target: "认领单 RC20261102007",
    operator: "admin",
  },
  {
    time: "11-02 09:58",
    action: "驳回认领申请",
    target: "认领单 RC20261102003",
    operator: "admin",
  },
  {
    time: "11-02 09:31",
    action: "审核通过招领信息",
    target: "招领单 LF20261102011",
    operator: "admin",
  },
  {
    time: "11-01 17:46",
    action: "更新交接状态",
    target: "认领单 RC20261101022",
    operator: "admin",
  },
  {
    time: "11-01 16:12",
    action: "发布系统公告",
    target: "《冬季失物保管期限调整》",
    operator: "admin",
  },
];

// 系统公告
const notices = [
  { title: "关于冬季遗失物品保管期限调整的通知", date: "2026-11-01" },
  { title: "国庆期间各车站失物招领库房值守安排", date: "2026-09-28" },
];

// 未开发模块统一提示
const goModule = (path) => {
  if (path === "/welcome") {
    router.push(path);
    return;
  }
  ElMessage.info("该管理模块开发中，敬请期待");
};
</script>

<template>
  <div class="workbench">
    <!-- 工作台横幅 -->
    <section class="banner">
      <div class="banner-text">
        <h2>{{ greeting }}，{{ username }}</h2>
        <p class="banner-sub">{{ roleText }} · {{ today }}</p>
        <p class="banner-tip">
          今日共有 <strong>{{ todoTotal }}</strong> 项待办需处理，其中认领审核为
          <strong>防冒领核心关卡</strong>，请优先核验实名信息与乘车凭证。
        </p>
      </div>
      <div class="banner-todo">
        <span class="todo-num">{{ todoTotal }}</span>
        <span class="todo-label">今日待办</span>
      </div>
    </section>

    <!-- 数据概览 -->
    <section class="block">
      <h3 class="block-title">数据概览</h3>
      <div class="stat-grid">
        <div
          v-for="item in stats"
          :key="item.key"
          class="stat-card"
          :class="{ accent: item.accent }"
        >
          <span class="stat-label">{{ item.label }}</span>
          <div class="stat-value">
            {{ item.value }}<span class="stat-unit">{{ item.unit }}</span>
          </div>
          <span class="stat-hint">{{ item.hint }}</span>
        </div>
      </div>
    </section>

    <div class="two-col">
      <!-- 待办事项 -->
      <section class="block">
        <h3 class="block-title">待办事项</h3>
        <div class="panel">
          <div v-for="item in todos" :key="item.title" class="todo-row">
            <span class="dot" :class="item.level"></span>
            <div class="todo-body">
              <div class="todo-head">
                <h4>{{ item.title }}</h4>
                <span class="todo-count">{{ item.count }} 条</span>
              </div>
              <p>{{ item.desc }}</p>
            </div>
            <el-button link type="primary" @click="goModule(item.path)">
              去处理
            </el-button>
          </div>
        </div>
      </section>

      <!-- 认领状态分布 -->
      <section class="block">
        <h3 class="block-title">认领状态分布</h3>
        <div class="panel">
          <p class="panel-tip">共 {{ claimTotal }} 条认领记录</p>
          <div v-for="item in claimStatus" :key="item.label" class="bar-row">
            <span class="bar-label">{{ item.label }}</span>
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{
                  width: (item.value / claimTotal) * 100 + '%',
                  background: item.color,
                }"
              ></div>
            </div>
            <span class="bar-value">{{ item.value }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- 快捷操作 -->
    <section class="block">
      <h3 class="block-title">快捷操作</h3>
      <div class="shortcut-grid">
        <button
          v-for="item in shortcuts"
          :key="item.title"
          class="shortcut"
          @click="goModule(item.path)"
        >
          <span class="shortcut-title">{{ item.title }}</span>
          <span class="shortcut-desc">{{ item.desc }}</span>
        </button>
      </div>
    </section>

    <div class="two-col">
      <!-- 最近操作日志 -->
      <section class="block">
        <h3 class="block-title">最近操作日志</h3>
        <div class="panel log-panel">
          <div v-for="(item, i) in logs" :key="i" class="log-row">
            <span class="log-time">{{ item.time }}</span>
            <span class="log-action">{{ item.action }}</span>
            <span class="log-target">{{ item.target }}</span>
          </div>
        </div>
      </section>

      <!-- 系统公告 -->
      <section class="block">
        <h3 class="block-title">系统公告</h3>
        <div class="panel">
          <div v-for="item in notices" :key="item.title" class="notice-row">
            <span class="notice-title">{{ item.title }}</span>
            <span class="notice-date">{{ item.date }}</span>
          </div>
          <el-button
            class="notice-more"
            link
            type="primary"
            @click="goModule('/notice')"
          >
            前往公告管理 →
          </el-button>
        </div>
      </section>
    </div>

    <!-- 业务提醒 -->
    <section class="notice">
      <h4>审核要点提醒</h4>
      <p>
        认领审核须交叉比对申请人的实名信息、车次行程与凭证内容；物品由乘务员官方保管的，
        核实后生成交接通知并指定车站服务台领取；由旅客自行保管的，核实双方身份后互发联系方式，
        由双方自行约定交接。驳回申请必须填写驳回理由，供失主查看。
      </p>
    </section>
  </div>
</template>

<style scoped>
.workbench {
  max-width: 1180px;
}

/* ============ 横幅 ============ */
.banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26px 34px;
  margin-bottom: 20px;
  border-radius: 10px;
  background: linear-gradient(135deg, #0e2f5c 0%, #14539e 65%, #1d63c9 100%);
  box-shadow: 0 6px 20px rgba(14, 47, 92, 0.18);
}

.banner-text h2 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #fff;
}

.banner-sub {
  margin: 0;
  font-size: 13px;
  color: rgba(220, 234, 255, 0.72);
}

.banner-tip {
  margin: 12px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(220, 234, 255, 0.88);
}

.banner-tip strong {
  color: #ffd98a;
  font-weight: 600;
}

.banner-todo {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 108px;
  padding: 12px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.todo-num {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
  color: #ffd98a;
}

.todo-label {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(220, 234, 255, 0.72);
}

/* ============ 通用区块 ============ */
.block {
  margin-bottom: 20px;
}

.block-title {
  margin: 0 0 14px;
  padding-left: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #0e2f5c;
  border-left: 3px solid #14539e;
  line-height: 1.2;
}

.panel {
  padding: 8px 20px;
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

/* ============ 数据概览 ============ */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  transition: all 0.2s;
}

.stat-card:hover {
  border-color: #b9cfe8;
  box-shadow: 0 6px 18px rgba(14, 47, 92, 0.08);
  transform: translateY(-2px);
}

.stat-card.accent {
  border-color: #f0d9b0;
  background: #fffdf7;
}

.stat-label {
  font-size: 13px;
  color: #71809a;
}

.stat-value {
  margin: 8px 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #0e2f5c;
  line-height: 1.2;
}

.stat-card.accent .stat-value {
  color: #c1781e;
}

.stat-unit {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #9aa7b8;
}

.stat-hint {
  font-size: 12px;
  color: #9aa7b8;
}

/* ============ 两栏 ============ */
.two-col {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 20px;
}

/* ============ 待办 ============ */
.todo-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px dashed #edf1f7;
}

.todo-row:last-child {
  border-bottom: none;
}

.dot {
  flex: 0 0 8px;
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: #c8d2de;
}

.dot.urgent {
  background: #e0a03c;
}

.todo-body {
  flex: 1;
  min-width: 0;
}

.todo-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.todo-head h4 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #26364d;
}

.todo-count {
  font-size: 11px;
  color: #c1781e;
  background: #fdf3e2;
  padding: 1px 7px;
  border-radius: 3px;
}

.todo-body p {
  margin: 5px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #8a97a8;
}

/* ============ 状态分布 ============ */
.panel-tip {
  margin: 10px 0 12px;
  font-size: 12px;
  color: #8a97a8;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.bar-label {
  flex: 0 0 52px;
  font-size: 12px;
  color: #4a5568;
}

.bar-track {
  flex: 1;
  height: 8px;
  background: #f0f3f8;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s;
}

.bar-value {
  flex: 0 0 30px;
  font-size: 12px;
  text-align: right;
  color: #26364d;
  font-weight: 600;
}

/* ============ 快捷操作 ============ */
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.shortcut {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 16px 20px;
  text-align: left;
  cursor: pointer;
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  transition: all 0.2s;
  font-family: inherit;
}

.shortcut:hover {
  border-color: #14539e;
  box-shadow: 0 6px 18px rgba(14, 47, 92, 0.1);
}

.shortcut-title {
  font-size: 13px;
  font-weight: 600;
  color: #0e2f5c;
}

.shortcut-desc {
  font-size: 12px;
  color: #9aa7b8;
}

/* ============ 日志与公告 ============ */
.log-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 0;
  font-size: 12px;
  border-bottom: 1px dashed #edf1f7;
}

.log-row:last-child {
  border-bottom: none;
}

.log-time {
  flex: 0 0 78px;
  color: #a0aab8;
}

.log-action {
  flex: 0 0 auto;
  padding: 1px 8px;
  color: #14539e;
  background: #eaf1fa;
  border-radius: 3px;
}

.log-target {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #4a5568;
}

.notice-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px dashed #edf1f7;
}

.notice-row:last-of-type {
  border-bottom: none;
}

.notice-title {
  font-size: 13px;
  color: #26364d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-date {
  flex: 0 0 auto;
  font-size: 12px;
  color: #a0aab8;
}

.notice-more {
  margin-top: 10px;
  font-size: 12px;
}

/* ============ 审核要点 ============ */
.notice {
  padding: 18px 22px;
  background: #fffdf5;
  border: 1px solid #f2e6c8;
  border-left: 3px solid #e0b64a;
  border-radius: 6px;
}

.notice h4 {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #8a6d1f;
}

.notice p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #7d6f52;
}

@media screen and (max-width: 900px) {
  .two-col {
    grid-template-columns: 1fr;
  }

  .banner-todo {
    display: none;
  }
}
</style>
