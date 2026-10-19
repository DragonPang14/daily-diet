<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";

const TOKEN_KEY = "token";
const ONBOARDING_PAGE = "/pages/onboarding/index";

onLaunch(() => {
  // 登录守卫：无 token 一律回到 onboarding（页内会自行处理登录/分流）
  const token = uni.getStorageSync(TOKEN_KEY) as string;
  if (!token) {
    uni.reLaunch({ url: ONBOARDING_PAGE });
  }
});
</script>
<style></style>
