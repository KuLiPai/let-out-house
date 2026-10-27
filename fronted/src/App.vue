<template>
  <RouterView />
  <ToastHost />
</template>

<script setup lang="ts">
import ToastHost from "./components/ToastHost.vue";
// 主题已在 main.ts 中通过 applyTheme() 初始化（默认夜间模式 + 动态配色）
</script>
