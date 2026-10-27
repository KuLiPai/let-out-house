import { reactive, readonly } from "vue";

export type ToastKind = "success" | "error" | "info";

export interface ToastItem {
  id: number;
  kind: ToastKind;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  duration: number;
}

const MAX = 4;
let seed = 0;

const state = reactive<{ items: ToastItem[] }>({ items: [] });

function push(kind: ToastKind, title: string, description?: string, opts: Partial<ToastItem> = {}) {
  const item: ToastItem = {
    id: ++seed,
    kind,
    title,
    description,
    duration: kind === "error" ? 6000 : 4000,
    ...opts,
  };
  state.items.push(item);
  if (state.items.length > MAX) state.items.shift();
  return item.id;
}

function dismiss(id: number) {
  const i = state.items.findIndex((t) => t.id === id);
  if (i !== -1) state.items.splice(i, 1);
}

export const toast = {
  success: (title: string, description?: string) => push("success", title, description),
  error: (title: string, description?: string) => push("error", title, description),
  info: (title: string, description?: string) => push("info", title, description),
  raw: push,
  dismiss,
};

export function useToast() {
  return { toasts: readonly(state).items, toast };
}
