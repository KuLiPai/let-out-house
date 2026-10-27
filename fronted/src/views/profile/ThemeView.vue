<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <!-- 预览 -->
    <section
      class="relative overflow-hidden rounded-[28px] border border-outline-variant/40 bg-gradient-to-br from-primary-container via-surface-container to-tertiary-container p-6 sm:p-8"
    >
      <div class="flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <ShinyText
            text="Material You"
            :speed="3"
            :color="palette['on-surface-variant']"
            :shine-color="palette.primary"
            class-name="text-sm font-medium tracking-widest uppercase"
          />
          <div class="mt-1 text-2xl sm:text-3xl font-semibold text-on-surface">动态配色</div>
          <p class="mt-2 max-w-md text-sm text-on-surface-variant">
            选择一个种子色，系统会按照 Material Design 3 规则自动生成整套主题色板，所有页面实时生效。
          </p>
        </div>
        <div class="flex shrink-0 gap-2">
          <span
            v-for="t in swatches"
            :key="t"
            class="h-12 w-12 sm:h-14 sm:w-14 rounded-2xl border border-outline-variant/40 shadow-sm transition-colors duration-500"
            :class="t"
          />
        </div>
      </div>
    </section>

    <!-- 明暗模式 -->
    <section class="m3-card p-5 sm:p-6">
      <div class="mb-1 font-medium text-on-surface">显示模式</div>
      <p class="mb-4 text-sm text-on-surface-variant">默认夜间模式，也可以跟随系统自动切换。</p>
      <div class="grid grid-cols-3 gap-2 sm:gap-3">
        <button
          v-for="m in modes"
          :key="m.value"
          type="button"
          class="group relative flex flex-col items-center gap-2 rounded-2xl border px-2 py-4 transition-colors cursor-pointer outline-none"
          :class="
            theme.mode === m.value
              ? 'border-primary bg-secondary-container text-on-secondary-container'
              : 'border-outline-variant text-on-surface-variant'
          "
          @click="setMode(m.value)"
        >
          <span class="pointer-events-none absolute inset-0 rounded-2xl bg-on-surface opacity-0 transition-opacity group-hover:opacity-[0.08] group-active:opacity-[0.12]" />
          <mdui-icon :name="m.icon" class="text-2xl"></mdui-icon>
          <span class="text-sm font-medium">{{ m.label }}</span>
        </button>
      </div>
    </section>

    <!-- 种子色 -->
    <section class="m3-card p-5 sm:p-6">
      <div class="mb-1 font-medium text-on-surface">主题色</div>
      <p class="mb-5 text-sm text-on-surface-variant">从预设中选择，或自定义颜色、从图片中提取。</p>

      <div class="grid grid-cols-4 gap-3 sm:grid-cols-8">
        <button
          v-for="c in PRESET_SEEDS"
          :key="c.value"
          type="button"
          class="group flex flex-col items-center gap-2 cursor-pointer outline-none"
          :title="c.label"
          @click="setSeed(c.value)"
        >
          <span
            class="relative inline-flex h-14 w-14 items-center justify-center rounded-full transition-transform duration-200 group-hover:scale-105 group-active:scale-95"
            :class="isCurrent(c.value) ? 'ring-2 ring-primary ring-offset-4 ring-offset-surface-container' : ''"
            :style="{ background: c.value }"
          >
            <Transition name="pop">
              <mdui-icon v-if="isCurrent(c.value)" name="check" class="text-[24px] text-white drop-shadow"></mdui-icon>
            </Transition>
          </span>
          <span class="text-xs text-on-surface-variant">{{ c.label }}</span>
        </button>
      </div>

      <div class="mt-6 flex flex-col gap-3 sm:flex-row">
        <label
          class="relative flex flex-1 cursor-pointer items-center gap-3 rounded-2xl border border-outline-variant px-4 py-3 transition-colors hover:bg-on-surface/[0.06]"
        >
          <span class="h-8 w-8 shrink-0 rounded-full border border-outline-variant" :style="{ background: theme.seed }" />
          <span class="flex-1">
            <span class="block text-sm font-medium text-on-surface">自定义颜色</span>
            <span class="block font-mono text-xs uppercase text-on-surface-variant">{{ theme.seed }}</span>
          </span>
          <mdui-icon name="colorize--outlined" class="text-on-surface-variant"></mdui-icon>
          <input
            type="color"
            class="absolute inset-0 h-full w-full cursor-pointer opacity-0"
            :value="theme.seed"
            @input="onColorInput"
          />
        </label>

        <label
          class="relative flex flex-1 cursor-pointer items-center gap-3 rounded-2xl border border-outline-variant px-4 py-3 transition-colors hover:bg-on-surface/[0.06]"
        >
          <span class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-tertiary-container text-on-tertiary-container">
            <mdui-icon name="wallpaper" class="text-[18px]"></mdui-icon>
          </span>
          <span class="flex-1">
            <span class="block text-sm font-medium text-on-surface">从图片取色</span>
            <span class="block text-xs text-on-surface-variant">上传壁纸，自动提取主色</span>
          </span>
          <mdui-circular-progress v-if="extracting" class="h-6 w-6"></mdui-circular-progress>
          <mdui-icon v-else name="upload" class="text-on-surface-variant"></mdui-icon>
          <input type="file" accept="image/*" class="absolute inset-0 h-full w-full cursor-pointer opacity-0" @change="onImage" />
        </label>
      </div>
    </section>

    <!-- 色板 -->
    <section class="m3-card p-5 sm:p-6">
      <div class="mb-4 font-medium text-on-surface">当前色板</div>
      <div class="grid grid-cols-2 gap-2 sm:grid-cols-4">
        <div
          v-for="t in tokens"
          :key="t.name"
          class="flex h-16 flex-col justify-end rounded-2xl p-3 text-xs font-medium transition-colors duration-500"
          :class="t.cls"
        >
          {{ t.name }}
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import ShinyText from "../../components/ShinyText.vue";
import { PRESET_SEEDS, useTheme, type ThemeMode } from "../../composables/useTheme";
import { toast } from "../../composables/useToast";

const { theme, palette, setMode, setSeed, setSeedFromImage } = useTheme();

const modes: { value: ThemeMode; label: string; icon: string }[] = [
  { value: "dark", label: "深色", icon: "dark_mode--outlined" },
  { value: "light", label: "浅色", icon: "light_mode--outlined" },
  { value: "auto", label: "跟随系统", icon: "brightness_auto--outlined" },
];

const swatches = ["bg-primary", "bg-secondary", "bg-tertiary"];

const tokens = [
  { name: "Primary", cls: "bg-primary text-on-primary" },
  { name: "Primary Container", cls: "bg-primary-container text-on-primary-container" },
  { name: "Secondary", cls: "bg-secondary text-on-secondary" },
  { name: "Secondary Container", cls: "bg-secondary-container text-on-secondary-container" },
  { name: "Tertiary", cls: "bg-tertiary text-on-tertiary" },
  { name: "Tertiary Container", cls: "bg-tertiary-container text-on-tertiary-container" },
  { name: "Surface Container", cls: "bg-surface-container-highest text-on-surface" },
  { name: "Error", cls: "bg-error text-on-error" },
];

const isCurrent = (v: string) => theme.seed.toLowerCase() === v.toLowerCase();

let colorTimer = 0;
const onColorInput = (e: Event) => {
  const v = (e.target as HTMLInputElement).value;
  clearTimeout(colorTimer);
  colorTimer = window.setTimeout(() => setSeed(v), 60);
};

const extracting = ref(false);
const onImage = async (e: Event) => {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  extracting.value = true;
  try {
    const hex = await setSeedFromImage(file);
    toast.success("已应用图片主色", hex.toUpperCase());
  } catch (err) {
    toast.error("取色失败", (err as Error).message);
  } finally {
    extracting.value = false;
    input.value = "";
  }
};
</script>

<style scoped>
.pop-enter-active,
.pop-leave-active {
  transition:
    transform 0.2s cubic-bezier(0.2, 0, 0, 1),
    opacity 0.2s ease;
}
.pop-enter-from,
.pop-leave-to {
  transform: scale(0.4);
  opacity: 0;
}
</style>
