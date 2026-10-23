<template>
    <mdui-layout class="h-screen w-screen">
        <mdui-top-app-bar class="p-4">
            <mdui-top-app-bar-title> 登录 </mdui-top-app-bar-title>
        </mdui-top-app-bar>

        <mdui-layout-main class="flex flex-col h-full w-full p-4">
            <div class="flex flex-col items-center justify-center h-full">
                <div class="flex flex-col justify-center w-fit h-fit">
                    <mdui-text-field
                        variant="outlined"
                        label="用户名"
                        :value="username"
                        @input="username = $event.target.value"
                    >
                    </mdui-text-field>

                    <div class="h-4"></div>

                    <mdui-text-field
                        variant="outlined"
                        label="密码"
                        type="password"
                        :value="password"
                        @input="password = $event.target.value"
                    >
                    </mdui-text-field>

                    <div class="h-4"></div>

                    <mdui-button @click="login()"> 登录 </mdui-button>
                </div>
            </div>
        </mdui-layout-main>
    </mdui-layout>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { setColorScheme, setTheme, snackbar } from "mdui";
import axios from "axios";

let username = ref("");
let password = ref("");

const login = () => {
    console.log(password.value);
};

setTheme("dark");
</script>

<style>
/* 消除浏览器默认的 body 边距 */
body {
    margin: 0;
    padding: 0;
    user-select: none; /* 让界面更有原生 App 的质感，防止鼠标拖拽选中文字 */
}
</style>
