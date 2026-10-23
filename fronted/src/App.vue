<script setup>
import { ref, computed } from 'vue'
import Home from './Home.vue'
import Login from './Login.vue'

const routes = {
  '/': Home,
  '/login': Login
}

const currentPath = ref(window.location.hash)
let currentView = routes["/"]
window.addEventListener('hashchange', () => {
  currentPath.value = window.location.hash
})

const isLogin = localStorage.getItem("isLogin")
if (isLogin === null) {
  currentView = routes["/login"]
}

//
// const currentView = computed(() => {
//   return routes[currentPath.value.slice(1) || '/'] || NotFound
// })



</script>

<template>
  <!-- <a href="#/">Home</a> |
  <a href="#/login">About</a> |
  <a href="#/non-existent-path">Broken Link</a> -->
  <component :is="currentView" />
</template>
