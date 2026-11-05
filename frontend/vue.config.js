const { defineConfig } = require("@vue/cli-service");
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  publicPath: "/",
  outputDir: "../src/main/resources/static/vue",
  devServer: {
    port: 8084, //前端端口8084

    proxy: {
      "/admin": {
        target: "http://localhost:8083",
        changeOrigin: true,
      },
      "/util": {
        target: "http://localhost:8083",
        changeOrigin: true,
      },
    },
  },
  chainWebpack: (config) => {
    config.plugin("html").tap((args) => {
      args[0].title = "高铁失物招领系统";
      return args;
    });
  },
});
