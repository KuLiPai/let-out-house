import { computed, reactive, readonly } from "vue";
import { setColorScheme, setTheme, getColorFromImage } from "mdui";

/**
 * 全局主题：Material Design 3 动态配色（mdui setColorScheme）+ 明暗模式。
 * - 默认夜间模式
 * - 种子色持久化在 localStorage("themeColor")
 * - palette 暴露解析后的 hex 颜色，给 canvas / WebGL 组件（DotField、ElectricLogo 等）使用，
 *   因为它们无法直接读取 CSS 变量。
 */

export type ThemeMode = "dark" | "light" | "auto";

export const DEFAULT_SEED = "#ad6dff";

export const PRESET_SEEDS = [
  { label: "星紫", value: "#ad6dff" },
  { label: "海蓝", value: "#4c8dff" },
  { label: "青碧", value: "#00b8a9" },
  { label: "薄荷", value: "#27ff64" },
  { label: "琥珀", value: "#f5a524" },
  { label: "珊瑚", value: "#ff6b6b" },
  { label: "樱粉", value: "#ff7eb6" },
  { label: "石墨", value: "#78909c" },
];

const TOKENS = [
  "primary",
  "on-primary",
  "primary-container",
  "on-primary-container",
  "secondary",
  "secondary-container",
  "tertiary",
  "tertiary-container",
  "surface",
  "surface-container",
  "surface-container-high",
  "on-surface",
  "on-surface-variant",
  "outline-variant",
  "error",
] as const;

type Token = (typeof TOKENS)[number];

const state = reactive({
  mode: (localStorage.getItem("themeMode") as ThemeMode) || "dark",
  seed: localStorage.getItem("themeColor") || DEFAULT_SEED,
  palette: {} as Record<Token, string>,
  /** 实际生效的明暗（auto 时跟随系统） */
  resolvedDark: true,
});

const toHex = (rgb: string) => {
  const parts = rgb.split(",").map((s) => parseInt(s.trim(), 10));
  if (parts.length < 3 || parts.some(Number.isNaN)) return "#000000";
  return "#" + parts.slice(0, 3).map((n) => n.toString(16).padStart(2, "0")).join("");
};

const media = window.matchMedia("(prefers-color-scheme: dark)");

const readPalette = () => {
  // mdui 同步写入 <style> 与 <html> class，这里可以立即读取解析后的变量
  const cs = getComputedStyle(document.documentElement);
  const next = {} as Record<Token, string>;
  for (const t of TOKENS) next[t] = toHex(cs.getPropertyValue(`--mdui-color-${t}`));
  state.palette = next;
  state.resolvedDark = state.mode === "auto" ? media.matches : state.mode === "dark";
  document.documentElement.style.colorScheme = state.resolvedDark ? "dark" : "light";
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute("content", next.surface);
};

media.addEventListener("change", () => {
  if (state.mode === "auto") readPalette();
});

export function applyTheme() {
  setTheme(state.mode);
  setColorScheme(state.seed);
  readPalette();
}

export function setMode(mode: ThemeMode) {
  state.mode = mode;
  localStorage.setItem("themeMode", mode);
  setTheme(mode);
  readPalette();
}

export function setSeed(hex: string) {
  state.seed = hex;
  localStorage.setItem("themeColor", hex);
  setColorScheme(hex);
  readPalette();
}

/** 从图片提取主色作为种子色（Material You 壁纸取色） */
export async function setSeedFromImage(file: File) {
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.src = url;
    await img.decode();
    const hex = await getColorFromImage(img);
    setSeed(hex);
    return hex;
  } finally {
    URL.revokeObjectURL(url);
  }
}

export function useTheme() {
  return {
    theme: readonly(state),
    isDark: computed(() => state.resolvedDark),
    palette: computed(() => state.palette),
    setMode,
    setSeed,
    setSeedFromImage,
  };
}
