<template>
  <div class="relative flex min-h-dvh w-full overflow-hidden bg-surface text-on-surface">
    <!-- 背景点阵（跟随主题色） -->
    <div class="absolute inset-0 z-0">
      <DotField
        :dot-radius="1.5"
        :dot-spacing="14"
        :bulge-strength="67"
        :glow-radius="160"
        :sparkle="false"
        :wave-amplitude="0"
        :cursor-radius="500"
        :cursor-force="0.1"
        bulge-only
        :gradient-from="palette['outline-variant']"
        :gradient-to="palette.primary"
        :glow-color="palette.surface"
      />
    </div>
    <div
      class="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(ellipse_at_center,transparent_0%,rgb(var(--mdui-color-surface))_80%)]"
    />

    <!-- 主题切换 -->
    <mdui-button-icon
      class="absolute right-3 top-[max(0.75rem,env(safe-area-inset-top))] z-20"
      :icon="isDark ? 'light_mode--outlined' : 'dark_mode--outlined'"
      @click="setMode(isDark ? 'light' : 'dark')"
    ></mdui-button-icon>

    <div
      class="relative z-10 mx-auto grid w-full max-w-6xl grid-cols-1 items-center gap-6 px-5 py-10 sm:px-8 md:grid-cols-2 md:gap-12 lg:px-12"
    >
      <!-- 品牌区 -->
      <section class="flex flex-col items-center text-center md:items-start md:text-left">
        <div class="relative h-[150px] w-[220px] sm:h-[200px] sm:w-[280px] md:h-[260px] md:w-[360px]">
          <ElectricLogo
            :src="logo"
            :color="palette.primary"
            :glow-color="palette.tertiary"
            :scale="0.7"
            :intensity="1"
            :glow="1"
            :thickness="1.5"
            :strands="4"
            :bend="0.6"
            :crackle="1.5"
            :arcs="1"
            :flicker="0.6"
            :fill="0"
            :speed="2.5"
            interactive
            :cursor-intensity="0.75"
            :cursor-radius="100"
            :theme="isDark ? 'dark' : 'light'"
          />
        </div>
        <GradientText
          :colors="[palette.primary, palette.tertiary, palette.secondary]"
          :animation-speed="6"
          class-name="!mx-0 !rounded-none !cursor-default text-3xl sm:text-4xl lg:text-5xl font-bold tracking-tight"
        >Let Out House</GradientText>
        <ShinyText
          text="你的 AI Agent 与知识库管理平台"
          :speed="3"
          :color="palette['on-surface-variant']"
          :shine-color="palette['on-surface']"
          class-name="mt-3 text-sm sm:text-base"
        />
      </section>

      <!-- 登录卡片 -->
      <section
        class="mx-auto w-full max-w-md rounded-[28px] border border-outline-variant/40 bg-surface-container/80 [--mdui-color-background:var(--mdui-color-surface-container)] p-6 shadow-2xl shadow-black/20 backdrop-blur-xl sm:p-8"
      >
        <h1 class="text-2xl font-semibold text-on-surface">欢迎回来</h1>
        <p class="mt-1 text-sm text-on-surface-variant">登录以继续使用</p>

        <form class="mt-6 flex flex-col gap-4" @submit.prevent="login">
          <mdui-text-field
            variant="outlined"
            label="用户名"
            icon="person--outlined"
            autocomplete="username"
            :value="username"
            @input="username = $event.target.value"
          ></mdui-text-field>

          <mdui-text-field
            variant="outlined"
            label="密码"
            type="password"
            toggle-password
            icon="lock--outlined"
            autocomplete="current-password"
            :value="password"
            @input="password = $event.target.value"
            @keydown.enter="login"
          ></mdui-text-field>

          <mdui-button type="submit" full-width :loading="loading" class="mt-2 h-12">登录</mdui-button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import axios from "axios";
import { useRouter } from "vue-router";
import ElectricLogo from "./components/ElectricLogo.vue";
import DotField from "./components/DotField.vue";
import GradientText from "./components/GradientText.vue";
import ShinyText from "./components/ShinyText.vue";
import logo from "./assets/logo.svg";
import { toast } from "./composables/useToast";
import { useTheme } from "./composables/useTheme";

const router = useRouter();
const { palette, isDark, setMode } = useTheme();

const username = ref("");
const password = ref("");
const loading = ref(false);

const login = async () => {
  if (loading.value) return;
  if (!username.value.trim() || !password.value) {
    toast.error("请输入用户名和密码");
    return;
  }
  loading.value = true;
  try {
    const res = await axios.post("/api/auth/login", {
      tenantCode: "platform",
      username: username.value,
      password: password.value,
    });

    const body = res.data;
    if (body.code === 0 && body.data?.token) {
      localStorage.setItem("token", body.data.token);
      localStorage.setItem("user", JSON.stringify(body.data));
      localStorage.setItem("isLogin", "true");
      toast.success("登录成功", body.data.nickname || body.data.username);
      router.replace("/");
    } else {
      toast.error("登录失败", body.msg || "用户名或密码错误");
    }
  } catch (err) {
    toast.error("登录失败", (err as Error).message);
  } finally {
    loading.value = false;
  }
};
</script>
