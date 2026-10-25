import { createApp } from "vue";
import App from "./App.vue";

import router from "./router";

// Material Icons 各变体字体（本地打包，Tauri 离线可用）
import "@fontsource/material-icons";
import "@fontsource/material-icons-outlined";
import "@fontsource/material-icons-round";
import "@fontsource/material-icons-sharp";
import "@fontsource/material-icons-two-tone";

// 引入 MDUI 2 核心样式与组件
import "mdui/mdui.css";
import "mdui";
import "./style.css";

import { applyTheme } from "./composables/useTheme";

// 挂载前应用主题（默认夜间 + 动态配色），避免首屏闪白
applyTheme();

const app = createApp(App);
app.use(router);
app.mount("#app");
