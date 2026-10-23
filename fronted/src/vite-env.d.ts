/// <reference types="vite/client" />
import "mdui/jsx.zh-cn.d.ts";

declare module "vue" {
  // 将 MDUI 的所有 Web Components 标签和属性类型映射到 Vue 的全局组件中
  export interface GlobalComponents extends React.JSX.IntrinsicElements {}
}
