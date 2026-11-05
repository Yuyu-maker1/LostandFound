module.exports = {
  root: true,
  env: {
    node: true,
    // Vue 3 <script setup> 编译宏（defineProps/defineEmits/defineExpose 等），避免 no-undef 误报
    "vue/setup-compiler-macros": true,
  },
  extends: [
    "plugin:vue/vue3-essential",
    "eslint:recommended",
    "plugin:prettier/recommended",
  ],
  parserOptions: {
    parser: "@babel/eslint-parser",
  },
  rules: {
    "no-console": process.env.NODE_ENV === "production" ? "warn" : "off",
    "no-debugger": process.env.NODE_ENV === "production" ? "warn" : "off",
    // 视图组件沿用 Index/Login/Welcome 等单位命名，属纯命名风格规则，关闭
    "vue/multi-word-component-names": "off",
  },
};
