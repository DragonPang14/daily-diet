import { defineStore } from "pinia";
import { ref } from "vue";
import type { UserInfo } from "@/api/user";

const TOKEN_KEY = "token";

export const useUserStore = defineStore("user", () => {
  const token = ref<string>(uni.getStorageSync(TOKEN_KEY) as string);
  const userInfo = ref<UserInfo | null>(null);
  const isNewUser = ref<boolean>(false);

  function login(payload: { token: string; isNewUser: boolean }): void {
    token.value = payload.token;
    isNewUser.value = payload.isNewUser;
    uni.setStorageSync(TOKEN_KEY, payload.token);
  }

  function setUser(info: UserInfo): void {
    userInfo.value = info;
  }

  function logout(): void {
    token.value = "";
    userInfo.value = null;
    isNewUser.value = false;
    uni.removeStorageSync(TOKEN_KEY);
  }

  function isLoggedIn(): boolean {
    return Boolean(token.value);
  }

  return { token, userInfo, isNewUser, login, setUser, logout, isLoggedIn };
});
