<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";

const route = useRoute();
const router = useRouter();

// 当前登录的管理员账号
const username = ref(localStorage.getItem("username") || "管理员");
const role = ref(localStorage.getItem("role") || "");
const isSuperAdmin = computed(() => role.value === "super_admin");
// 显式映射，避免把未知/空角色兜底成“车站管理员”
const roleText = computed(() => {
  if (role.value === "super_admin") return "超级管理员";
  if (role.value === "admin") return "车站管理员";
  return "未知角色";
});

// 当前激活菜单与面包屑标题
const activeMenu = computed(() => route.path);
const currentTitle = computed(() => route.meta.title || "工作台");

// 管理端菜单分组（依据论文 3.1.4 车站管理员模块需求划分）
const menuGroups = computed(() => {
  const groups = [
    {
      title: "概览",
      items: [{ path: "/welcome", title: "工作台" }],
    },
    {
      title: "信息审核",
      items: [
        { path: "/audit/claim", title: "认领申请审核" },
        { path: "/audit/found", title: "招领信息审核" },
        { path: "/audit/lost", title: "寻物启事审核" },
      ],
    },
    {
      title: "业务管理",
      items: [
        { path: "/handover", title: "交接与归档" },
        { path: "/notice", title: "公告管理" },
      ],
    },
    {
      title: "用户与基础数据",
      items: [
        { path: "/user", title: "用户管理" },
        { path: "/train", title: "车次信息核对" },
      ],
    },
    {
      title: "数据与安全",
      items: [
        { path: "/statistics", title: "数据统计" },
        { path: "/logs", title: "操作日志" },
      ],
    },
  ];
  // 仅超级管理员可见管理员管理入口
  if (isSuperAdmin.value) {
    groups.push({
      title: "系统管理",
      items: [{ path: "/admins", title: "管理员管理" }],
    });
  }
  return groups;
});

// 已开发完成的页面路径
const readyPaths = ["/welcome", "/userinfo", "/admins"];

const handleSelect = (path) => {
  if (readyPaths.includes(path)) {
    router.push(path);
  } else {
    ElMessage.info("该管理模块开发中，敬请期待");
  }
};

// 退出登录
const handleLogout = () => {
  localStorage.removeItem("username");
  localStorage.removeItem("role");
  localStorage.removeItem("token");
  ElMessage.success("已退出登录");
  router.push("/login");
};
</script>

<template>
  <el-container class="layout">
    <!-- 顶部导航 -->
    <el-header class="layout-header">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none">
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
        </div>
        <span class="sys-name">高铁失物招领系统</span>
        <span class="sys-tag">管理后台</span>
      </div>

      <div class="header-right">
        <span class="role-tag">{{ roleText }}</span>
        <el-dropdown trigger="click">
          <div class="user-box">
            <span class="user-name">{{ username }}</span>
            <span class="caret">▾</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleSelect('/userinfo')">
                账号与权限
              </el-dropdown-item>
              <el-dropdown-item @click="handleSelect('/logs')">
                操作日志
              </el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="layout-body">
      <!-- 侧边栏 -->
      <el-aside width="208px" class="layout-aside">
        <el-menu :default-active="activeMenu" class="side-menu">
          <el-menu-item-group
            v-for="group in menuGroups"
            :key="group.title"
            :title="group.title"
          >
            <el-menu-item
              v-for="item in group.items"
              :key="item.path"
              :index="item.path"
              @click="handleSelect(item.path)"
            >
              {{ item.title }}
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>

        <div class="aside-footer">管理端 v1.0.0 · 2026</div>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="layout-main">
        <div class="crumb">
          <span>后台管理</span>
          <span class="sep">/</span>
          <span class="current">{{ currentTitle }}</span>
        </div>

        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

/* ============ 顶部 ============ */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px 0 20px;
  background: #0e2f5c;
  box-shadow: 0 2px 8px rgba(7, 28, 54, 0.28);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
}

.sys-name {
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #fff;
}

.sys-tag {
  padding: 2px 8px;
  font-size: 10px;
  letter-spacing: 1px;
  color: rgba(220, 234, 255, 0.62);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 3px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.role-tag {
  padding: 2px 9px;
  font-size: 11px;
  color: #bcd6f5;
  background: rgba(29, 99, 201, 0.28);
  border: 1px solid rgba(120, 170, 235, 0.35);
  border-radius: 3px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 12px;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-box:hover {
  background: rgba(255, 255, 255, 0.08);
}

.user-name {
  font-size: 13px;
  color: #dbe8f8;
}

.caret {
  font-size: 10px;
  color: rgba(219, 232, 248, 0.6);
}

/* ============ 主体 ============ */
.layout-body {
  overflow: hidden;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  background: #f7f9fc;
  border-right: 1px solid #e6ebf2;
  overflow-y: auto;
}

.side-menu {
  flex: 1;
  padding-top: 8px;
  border-right: none;
  background: transparent;
}

.side-menu :deep(.el-menu-item-group__title) {
  padding: 12px 0 6px 20px !important;
  font-size: 11px;
  letter-spacing: 1px;
  color: #9aa7b8;
}

.side-menu :deep(.el-menu-item) {
  height: 44px;
  margin: 2px 10px;
  font-size: 14px;
  color: #4a5568;
  border-radius: 6px;
}

.side-menu :deep(.el-menu-item:hover) {
  color: #0e2f5c;
  background: #eaf1fa;
}

.side-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #14539e;
}

.aside-footer {
  padding: 14px 20px;
  font-size: 11px;
  color: #a8b3c2;
  border-top: 1px solid #e6ebf2;
}

.layout-main {
  padding: 20px 24px;
  overflow-y: auto;
  background: #f0f3f8;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #8a97a8;
}

.crumb .sep {
  color: #c8d2de;
}

.crumb .current {
  color: #0e2f5c;
  font-weight: 500;
}
</style>
