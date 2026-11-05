import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

// 本地开发默认代理到 8083；如后端临时跑在其它端口，可用 API_TARGET 覆盖，例如：
// PowerShell: $env:API_TARGET='http://localhost:18083'; npm run dev:h5
const apiTarget = process.env.API_TARGET || 'http://localhost:8083'

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 8090,
    host: '0.0.0.0',
    proxy: {
      '/app': {
        target: apiTarget,
        changeOrigin: true
      },
      // 与管理端保持同一代理目标，避免本地联调时管理端请求 404
      '/admin': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  }
})
