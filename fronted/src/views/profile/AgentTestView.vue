<template>
  <div class="mx-auto flex h-full min-h-0 max-w-4xl flex-col gap-4">
    <!-- Agent 选择 -->
    <div class="flex shrink-0 flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <p class="page-subtitle !mt-0 hidden sm:block">会话接口接通前，这里走本地模拟流程。</p>
      <mdui-select
        variant="outlined"
        label="选择 Agent"
        icon="smart_toy--outlined"
        :value="selectedId == null ? '' : String(selectedId)"
        class="w-full sm:max-w-xs"
        @change="selectedId = Number($event.target.value)"
      >
        <mdui-menu-item v-for="a in agents" :key="a.agentId" :value="String(a.agentId)">
          {{ a.agentName }}
        </mdui-menu-item>
      </mdui-select>
    </div>

    <!-- 对话区 -->
    <div ref="listRef" class="m3-card min-h-0 flex-1 space-y-4 overflow-y-auto p-4 sm:p-6">
      <div v-if="!messages.length && !thinking" class="flex h-full flex-col items-center justify-center gap-3 text-center">
        <span class="inline-flex h-16 w-16 items-center justify-center rounded-3xl bg-primary-container text-on-primary-container">
          <mdui-icon name="forum--outlined" class="text-[30px]"></mdui-icon>
        </span>
        <ShinyText
          text="还没有对话，试试问点什么"
          :speed="3"
          :color="palette['on-surface-variant']"
          :shine-color="palette.primary"
          class-name="text-sm"
        />
        <div class="mt-2 flex flex-wrap justify-center gap-2">
          <button
            v-for="q in suggestions"
            :key="q"
            type="button"
            class="rounded-full border border-outline-variant px-3 py-1.5 text-xs text-on-surface-variant transition-colors hover:bg-on-surface/[0.08] cursor-pointer"
            @click="input = q; send()"
          >
            {{ q }}
          </button>
        </div>
      </div>

      <TransitionGroup name="msg">
        <div
          v-for="m in messages"
          :key="m.id"
          class="flex items-end gap-2"
          :class="m.role === 'user' ? 'flex-row-reverse' : ''"
        >
          <span
            class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm"
            :class="m.role === 'user' ? 'bg-primary text-on-primary' : 'bg-tertiary-container text-on-tertiary-container'"
          >
            <mdui-icon :name="m.role === 'user' ? 'person' : 'smart_toy--outlined'" class="text-[18px]"></mdui-icon>
          </span>
          <div
            class="selectable max-w-[80%] whitespace-pre-wrap break-words px-4 py-2.5 text-sm leading-relaxed"
            :class="
              m.role === 'user'
                ? 'rounded-3xl rounded-br-md bg-primary-container text-on-primary-container'
                : 'rounded-3xl rounded-bl-md bg-surface-container-highest text-on-surface'
            "
          >
            {{ m.text }}
          </div>
        </div>
      </TransitionGroup>

      <ThoughtLine
        v-if="thinking"
        label="正在检索知识库并生成回答…"
        done-label="已完成"
        :steps="steps"
        :working="thinking"
        :font-size="14"
        :settle-after="0"
        :color="palette['on-surface']"
        :glyph-color="palette.primary"
        class="pl-10 pt-2"
      />
    </div>

    <!-- 输入栏 -->
    <div class="flex shrink-0 items-end gap-2 rounded-[28px] bg-surface-container-high p-2 pl-4">
      <mdui-text-field
        variant="filled"
        placeholder="输入消息，Enter 发送"
        class="composer flex-1"
        :value="input"
        :disabled="thinking"
        @input="input = $event.target.value"
        @keydown.enter.exact.prevent="send"
      ></mdui-text-field>
      <mdui-fab
        size="small"
        icon="send"
        class="shrink-0"
        :disabled="thinking || !input.trim()"
        @click="send"
      ></mdui-fab>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from "vue";
import ThoughtLine from "../../components/ThoughtLine.vue";
import ShinyText from "../../components/ShinyText.vue";
import { useTheme } from "../../composables/useTheme";
import { agentApi, type AgentResponse } from "../../api/agent";
import { toast } from "../../composables/useToast";

interface Msg {
  id: number;
  role: "user" | "assistant";
  text: string;
}

const agents = ref<AgentResponse[]>([]);
const selectedId = ref<number | null>(null);
const messages = ref<Msg[]>([]);
const input = ref("");
const thinking = ref(false);
const steps = ref<string[]>([]);
let seed = 0;

const { palette } = useTheme();
const listRef = ref<HTMLElement | null>(null);
const suggestions = ["你能做什么？", "帮我总结一下知识库", "介绍一下你自己"];

// 新消息 / 思考步骤出现时自动滚到底部
watch(
  () => [messages.value.length, steps.value.length, thinking.value],
  async () => {
    await nextTick();
    listRef.value?.scrollTo({ top: listRef.value.scrollHeight, behavior: "smooth" });
  }
);

onMounted(async () => {
  try {
    const page = await agentApi.list({ pageNum: 1, pageSize: 100 });
    agents.value = page.list;
    selectedId.value = page.list[0]?.agentId ?? null;
  } catch (e) {
    toast.error("加载 Agent 失败", (e as Error).message);
  }
});

const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));

const send = async () => {
  const text = input.value.trim();
  if (!text || thinking.value) return;
  if (!selectedId.value) return toast.error("请先选择 Agent");

  messages.value.push({ id: ++seed, role: "user", text });
  input.value = "";
  thinking.value = true;
  steps.value = [];

  // 会话接口尚未开放，先本地模拟检索过程
  const trace = ["解析问题", "检索知识库", "组织上下文", "生成回答"];
  for (const s of trace) {
    steps.value.push(s);
    await wait(600);
  }

  messages.value.push({
    id: ++seed,
    role: "assistant",
    text: "（会话接口未接入）这是模拟回复。后端 `/api/conversation` 开放后，替换 send() 内的模拟逻辑即可。",
  });
  thinking.value = false;
};
</script>

<style scoped>
/* 输入框融入胶囊输入栏 */
.composer::part(container) {
  background: transparent;
  box-shadow: none;
  border-radius: 0;
  min-height: 2.5rem;
}
.msg-enter-active {
  transition:
    opacity 0.25s ease,
    transform 0.3s cubic-bezier(0.2, 0, 0, 1);
}
.msg-enter-from {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}
</style>
