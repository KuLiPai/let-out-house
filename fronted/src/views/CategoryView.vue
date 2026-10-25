<template>
  <div class="space-y-6">
    <p class="page-subtitle !mt-0">按用途浏览你的 Agent 与知识库。</p>

    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 sm:gap-4">
      <SpotlightCard
        v-for="c in categories"
        :key="c.title"
        class-name="!p-5 !rounded-3xl !border-outline-variant/40 bg-surface-container"
        :spotlight-color="`${palette.primary}2e`"
      >
        <div class="flex items-start gap-4">
          <span class="inline-flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl" :class="c.tone">
            <mdui-icon :name="c.icon" class="text-[24px]"></mdui-icon>
          </span>
          <div class="min-w-0">
            <div class="font-medium text-on-surface">{{ c.title }}</div>
            <div class="mt-1 text-sm text-on-surface-variant">{{ c.desc }}</div>
          </div>
        </div>
        <div class="mt-5 flex items-center justify-between">
          <span class="rounded-full bg-surface-container-highest px-3 py-1 text-xs text-on-surface-variant">即将上线</span>
          <mdui-icon name="arrow_forward" class="text-on-surface-variant"></mdui-icon>
        </div>
      </SpotlightCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import SpotlightCard from "../components/SpotlightCard.vue";
import { useTheme } from "../composables/useTheme";

const { palette } = useTheme();

// 分类功能后端尚未提供，先展示占位卡片
const categories = [
  { title: "客服问答", desc: "面向用户的常见问题解答", icon: "support_agent", tone: "bg-primary-container text-on-primary-container" },
  { title: "知识检索", desc: "基于知识库的 RAG 检索", icon: "travel_explore", tone: "bg-tertiary-container text-on-tertiary-container" },
  { title: "内容创作", desc: "文案、摘要与改写", icon: "edit_note", tone: "bg-secondary-container text-on-secondary-container" },
  { title: "数据分析", desc: "表格与报表解读", icon: "insights", tone: "bg-secondary-container text-on-secondary-container" },
  { title: "代码助手", desc: "代码生成与审查", icon: "code", tone: "bg-primary-container text-on-primary-container" },
  { title: "更多", desc: "自定义你的分类", icon: "add_circle_outline", tone: "bg-surface-container-highest text-on-surface" },
];
</script>
