<template>
  <div class="w-full">
    <div class="flex items-baseline justify-between mb-1">
      <span class="text-sm opacity-70">{{ label }}</span>
      <span class="text-sm font-mono tabular-nums">{{ display }}</span>
    </div>
    <ElasticSlider
      ref="sliderRef"
      :default-value="modelValue"
      :starting-value="min"
      :max-value="max"
      :is-stepped="isStepped"
      :step-size="step"
      :left-icon="'−'"
      :right-icon="'+'"
      class="!w-full"
    />
    <div v-if="hint" class="text-xs opacity-40 -mt-2">{{ hint }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount } from "vue";
import ElasticSlider from "./ElasticSlider.vue";

const props = withDefaults(
  defineProps<{
    modelValue: number;
    min?: number;
    max?: number;
    step?: number;
    isStepped?: boolean;
    label?: string;
    hint?: string;
    decimals?: number;
  }>(),
  { min: 0, max: 100, step: 1, isStepped: false, label: "", hint: "", decimals: 0 }
);

const emit = defineEmits<{ "update:modelValue": [number] }>();

const sliderRef = ref<InstanceType<typeof ElasticSlider> | null>(null);
let raf = 0;
let last = props.modelValue;

const display = computed(() => Number(props.modelValue).toFixed(props.decimals));

// ElasticSlider 内部只更新自己的 value，这里轮询同步回父组件
const tick = () => {
  const v = sliderRef.value?.value;
  if (typeof v === "number") {
    const rounded = props.isStepped
      ? Math.round(v / props.step) * props.step
      : Number(v.toFixed(props.decimals));
    if (rounded !== last) {
      last = rounded;
      emit("update:modelValue", rounded);
    }
  }
  raf = requestAnimationFrame(tick);
};

watch(
  () => props.modelValue,
  (v) => {
    last = v;
  }
);

onMounted(() => {
  raf = requestAnimationFrame(tick);
});
onBeforeUnmount(() => cancelAnimationFrame(raf));
</script>
