import { createRouter, createWebHistory } from "vue-router";
import FindView from "@/views/FindView.vue";
import Login from "@/views/Login.vue";
import Index from "@/views/Index.vue";
import Welcome from "@/views/Welcome.vue";
import UserInfo from "@/views/UserInfo.vue";
import AdminManage from "@/views/AdminManage.vue";

const routes = [
  {
    // 根路径直接进入登录页
    path: "/",
    redirect: "/login",
  },
  {
    path: "/about",
    name: "about",
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () =>
      import(/* webpackChunkName: "about" */ "../views/AboutView.vue"),
  },
  {
    path: "/findtop",
    name: "findtop",
    component: FindView,
  },
  {
    path: "/login",
    name: "login",
    component: Login,
  },
  {
    // 系统首页（主框架）
    path: "/index",
    name: "index",
    component: Index,
    meta: { requiresAuth: true },
    redirect: "/welcome",
    children: [
      {
        // 欢迎页面路由
        path: "/welcome",
        name: "welcome",
        component: Welcome,
        meta: { title: "工作台" },
      },
      {
        path: "/userinfo",
        name: "userinfo",
        component: UserInfo,
      },
      {
        // 管理员管理：仅超级管理员可访问（路由守卫 + 后端 403 双重校验）
        path: "/admins",
        name: "admins",
        component: AdminManage,
        meta: { title: "管理员管理", requiresSuper: true },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

// 添加路由守卫
router.beforeEach((to, from, next) => {
  // 检查路由是否需要认证
  if (to.matched.some((record) => record.meta.requiresAuth)) {
    // 检查是否存在token
    const token = localStorage.getItem("token");
    if (!token) {
      // 没有token，重定向到登录页
      next({ path: "/login" });
      return;
    }
    // 仅超级管理员可访问的路由，直接输入地址也会被拦回工作台
    if (
      to.matched.some((record) => record.meta.requiresSuper) &&
      localStorage.getItem("role") !== "super_admin"
    ) {
      next({ path: "/welcome" });
      return;
    }
    // 有token，继续访问
    next();
  } else {
    // 不需要认证的路由，直接通过
    next();
  }
});

export default router;
