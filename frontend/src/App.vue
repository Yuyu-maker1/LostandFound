<template>
  <!--  <nav>-->
  <!--    <router-link to="/">Home</router-link> |-->
  <!--    <router-link to="/about">About</router-link>-->
  <!--  </nav>-->
  <router-view />
</template>

<style>
/* 重置全局样式，避免默认样式干扰页面布局 */
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
}

#app {
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Helvetica,
    Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1f2d3d;
}

* {
  box-sizing: border-box;
}
</style>
