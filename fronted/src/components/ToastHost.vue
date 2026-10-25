<template>
  <div
    class="pointer-events-none fixed inset-x-3 z-50 flex flex-col-reverse items-center gap-3 sm:inset-x-auto sm:right-4 sm:items-end md:bottom-4"
    :class="hasBottomNav ? 'bottom-[calc(5.75rem+env(safe-area-inset-bottom))]' : 'bottom-[calc(1rem+env(safe-area-inset-bottom))]'"
  >
    <SwipeToast
      v-for="t in items"
      :key="t.id"
      class="pointer-events-auto max-w-full"
      :title="t.title"
      :description="t.description"
      :action-label="t.actionLabel"
      background="rgb(var(--mdui-color-inverse-surface))"
      color="rgb(var(--mdui-color-inverse-on-surface))"
      :fuse-color="accentOf(t.kind)"
      :radius="16"
      :width="width"
      :duration="t.duration"
      close-button
      @action="t.onAction?.()"
      @close="() => dismiss(t.id)"
    >
      <template #icon>
        <span class="inline-block w-[18px] h-[18px] rounded-full" :style="{ background: accentOf(t.kind) }" />
      </template>
    </SwipeToast>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from "vue";
import { useRoute } from "vue-router";
import SwipeToast from "./SwipeToast.vue";
import { useToast, type ToastKind } from "../composables/useToast";

const { toasts: items, toast } = useToast();
const dismiss = toast.dismiss;
const route = useRoute();

// 登录页没有底部导航栏，手机上贴底显示
const hasBottomNav = computed(() => !route.meta.public);

// 手机上 toast 宽度不超过屏幕
const vw = ref(window.innerWidth);
const onResize = () => (vw.value = window.innerWidth);
window.addEventListener("resize", onResize);
onBeforeUnmount(() => window.removeEventListener("resize", onResize));
const width = computed(() => Math.min(356, vw.value - 24));

const accentOf = (kind: ToastKind) =>
  kind === "success"
    ? "#4ade80"
    : kind === "error"
      ? "rgb(var(--mdui-color-error))"
      : "rgb(var(--mdui-color-inverse-primary))";
</script>
