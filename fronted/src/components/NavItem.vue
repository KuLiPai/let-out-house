<template>
  <button
    type="button"
    class="nav-item group relative flex flex-col items-center justify-center gap-1 outline-none select-none cursor-pointer"
    :class="compact ? 'flex-1 h-full' : 'w-full py-1.5'"
    :data-active="active || undefined"
    :aria-current="active ? 'page' : undefined"
    @click="$emit('select')"
  >
    <!--
      指示器与悬停层使用同一个胶囊形状和尺寸，
      保证 hover / pressed / selected 三种状态视觉一致（MD3 Navigation 规范）。
    -->
    <span class="relative flex items-center justify-center h-8 rounded-full overflow-hidden" :class="compact ? 'w-16' : 'w-14'">
      <!-- selected：secondary-container 胶囊，从中心横向展开 -->
      <span
        class="absolute inset-0 rounded-full bg-secondary-container origin-center scale-x-[0.3] opacity-0 transition-[transform,opacity] duration-300 ease-[cubic-bezier(0.2,0,0,1)] group-data-[active]:scale-x-100 group-data-[active]:opacity-100"
      />
      <!-- state layer：hover 8% / pressed 12% / focus 12% -->
      <span
        class="absolute inset-0 rounded-full bg-on-surface opacity-0 transition-opacity duration-150 group-hover:opacity-[0.08] group-active:opacity-[0.12] group-focus-visible:opacity-[0.12]"
      />
      <mdui-icon
        :name="active ? icon : `${icon}--outlined`"
        class="relative text-[24px] leading-none transition-colors"
        :class="active ? 'text-on-secondary-container' : 'text-on-surface-variant'"
      ></mdui-icon>
    </span>
    <span
      class="text-xs leading-4 tracking-wide transition-colors"
      :class="active ? 'text-on-surface font-semibold' : 'text-on-surface-variant font-medium'"
    >
      {{ label }}
    </span>
    <!-- 键盘焦点环 -->
    <span
      class="pointer-events-none absolute inset-0 rounded-2xl ring-2 ring-primary opacity-0 group-focus-visible:opacity-100"
    />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  /** 填充图标名，未选中时自动使用 `--outlined` 变体 */
  icon: string;
  label: string;
  active?: boolean;
  /** 底部导航栏模式（手机） */
  compact?: boolean;
}>();

defineEmits<{ select: [] }>();
</script>
