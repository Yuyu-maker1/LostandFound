import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import "element-plus/dist/index.css";

// 注册 Element Plus 组件库，并设置中文语言包
createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount("#app");

// const app = createApp(App);
// app.use(router);
// app.use(ElementPlus, { locale: zhCn });
// app.mount("#app");
