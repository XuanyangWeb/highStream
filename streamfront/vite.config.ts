import vue from '@vitejs/plugin-vue'
import path from 'path'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  server: {
    proxy: {
      '/api':{
        target: 'http://localhost:8081'
      }
    }
  },
  plugins: [
    vue(),
    tailwindcss(),

  ],
  resolve :{
    alias: {
      '@':path.resolve(__dirname, './src')
    }
  }
})
