import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import "element-plus/dist/index.css";

const app = createApp(App);

app.use(router);
// 关键：注册 Element Plus 组件库，并设置中文语言包
app.use(ElementPlus, { locale: zhCn });

app.mount("#app");
