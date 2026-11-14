<script setup lang="ts">


</script>


<template>
  <div class="app-container">
    <!-- 全局导航栏 -->
    <NavBar />
    
    <main class="main-content">
      <!-- 路由视图：根据 URL 动态渲染 Home.vue 或其他页面 -->
      <RouterView />
    </main>
    
    <!-- 全局页脚 -->
    <Footer />
  </div>
</template>

<style scoped>
/* 列表过渡动画 */
.list-enter-active, .list-leave-active { transition: all 0.4s cubic-bezier(0.18, 0.89, 0.32, 1.28); }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(30px); }
</style>