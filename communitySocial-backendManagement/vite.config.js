import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
    build: {
        // 打包输出目录，默认是 dist，无需修改
        outDir: 'dist',
        // 清空打包目录后再输出（避免旧文件残留）
        emptyOutDir: true
    },
    server: {
      port: 5173,
      proxy: {
        '/admin': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true
        },
        '/resident': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true
        },
        '/image': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true
        },
        '/actuator': {
          target: 'http://127.0.0.1:8080',
          changeOrigin: true
        }
      }
    }
})
