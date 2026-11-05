<script>
export default {
  onLaunch() {
    // 首版只做登录页与首页，启动无需额外初始化
  }
}
</script>

<style>
page {
  background-color: #f5f7fb;
  color: #1f2937;
  font-size: 28rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

view,
text {
  box-sizing: border-box;
}

/* uni-app 的 input/textarea 自定义元素默认是 content-box，
   加左右 padding 后宽度会超出卡片，这里统一改为 border-box */
uni-input,
uni-textarea {
  box-sizing: border-box;
}
</style>
