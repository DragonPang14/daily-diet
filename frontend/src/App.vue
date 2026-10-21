<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { authApi } from "@/api/auth";
import { useUserStore } from "@/store/user";

const TOKEN_KEY = "token";
const ONBOARDING_PAGE = "/pages/onboarding/index";

/**
 * 应用启动入口：
 * - 已持有 token → 直接放行，进入默认 tabBar 首页
 * - 无 token → 后台静默尝试微信登录，成功则存 token；失败静默忽略
 * - 登录成功且 isNewUser → 强制进入 Onboarding 引导流程
 * - 登录成功但老用户 / 登录失败 → 直接进入首页，公共内容（食物库等）无需登录即可浏览
 *
 * 原则：登录成功或失败都不阻塞首页展示。
 */
onLaunch(async () => {
  const store = useUserStore();
  const existingToken = uni.getStorageSync(TOKEN_KEY) as string;
  if (existingToken) {
    store.token = existingToken;
    return;
  }

  try {
    const res = await uni.login({ provider: "weixin" });
    if (!res.code) {
      // 没拿到 code（非微信环境或用户拒绝），静默放行
      return;
    }
    const r = await authApi.wxLogin(res.code);
    store.login(r);
    if (r.isNewUser) {
      uni.reLaunch({ url: ONBOARDING_PAGE });
    }
  } catch {
    // 网络/后端异常：静默忽略，应用自然进入首页
  }
});
</script>
<style></style>
