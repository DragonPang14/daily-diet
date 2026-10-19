<template>
  <view class="mine">
    <!-- 资料卡 -->
    <view class="card profile" @click="goEdit(1)">
      <view class="avatar">{{ avatarText }}</view>
      <view class="pinfo">
        <view class="pname">{{ info?.nickname || "微信用户" }}</view>
        <view class="pmeta">{{ metaText }}</view>
      </view>
      <text class="arrow">›</text>
    </view>

    <!-- 热量四格 -->
    <view class="card grid4">
      <view class="cell">
        <text class="num">{{ round(target?.bmr) }}</text>
        <text class="label">BMR kcal</text>
      </view>
      <view class="cell">
        <text class="num">×{{ muscleFactorText }}</text>
        <text class="label">肌肉修正</text>
      </view>
      <view class="cell">
        <text class="num">{{ round(target?.tdee) }}</text>
        <text class="label">TDEE kcal</text>
      </view>
      <view class="cell">
        <text class="num primary">{{ round(target?.targetKcal) }}</text>
        <text class="label">目标 kcal</text>
      </view>
    </view>

    <!-- 当前目标 + PFC -->
    <view class="card" v-if="target">
      <view class="goal-head">
        <text>当前目标：{{ goalLabel }}</text>
        <text class="goal-delta">{{ goalDeltaText }}</text>
      </view>
      <view class="pfc-bar">
        <view class="seg carbo" :style="{ width: target.pfcCarboPct + '%' }"></view>
        <view class="seg protein" :style="{ width: target.pfcProteinPct + '%' }"></view>
        <view class="seg fat" :style="{ width: target.pfcFatPct + '%' }"></view>
      </view>
      <view class="pfc-labels">
        <text>碳水 {{ target.pfcCarboPct }}%</text>
        <text>蛋白 {{ target.pfcProteinPct }}%</text>
        <text>脂肪 {{ target.pfcFatPct }}%</text>
      </view>
    </view>

    <!-- 菜单：资料管理 -->
    <view class="menu">
      <view class="mi" @click="goEdit(1)">
        <text class="icn">👤</text><text class="mt">基础信息</text>
        <text class="val">体重变化自动重算</text><text class="arr">›</text>
      </view>
      <view class="mi" @click="goEdit(2)">
        <text class="icn">🏋️</text><text class="mt">训练数据</text>
        <text class="val">{{ trainingText }}</text><text class="arr">›</text>
      </view>
      <view class="mi" @click="goEdit(3)">
        <text class="icn">🎯</text><text class="mt">目标与 PFC 比例</text>
        <text class="arr">›</text>
      </view>
      <view class="mi" @click="comingSoon">
        <text class="icn">👎</text><text class="mt">不喜欢的食材</text>
        <text class="arr">›</text>
      </view>
      <view class="mi" @click="comingSoon">
        <text class="icn">⭐</text><text class="mt">收藏 / 常用食物</text>
        <text class="badge-ver">v1.1</text><text class="arr">›</text>
      </view>
    </view>

    <!-- 菜单：其他 -->
    <view class="menu">
      <view class="mi" @click="showFormula = true">
        <text class="icn">📐</text><text class="mt">公式说明（BMR/TDEE）</text>
        <text class="arr">›</text>
      </view>
      <view class="mi" @click="onLogout">
        <text class="icn">🚪</text><text class="mt">退出登录</text>
        <text class="arr">›</text>
      </view>
    </view>

    <!-- 公式说明弹层 -->
    <u-popup :show="showFormula" mode="center" round="16" @close="showFormula = false">
      <view class="formula">
        <view class="f-title">热量计算规则</view>
        <view class="f-block">
          <view class="f-h">① BMR 基础代谢（Mifflin-St Jeor）</view>
          <view class="f-p">男：10×体重 + 6.25×身高 − 5×年龄 + 5</view>
          <view class="f-p">女：10×体重 + 6.25×身高 − 5×年龄 − 161</view>
        </view>
        <view class="f-block">
          <view class="f-h">② 肌肉修正（三大项 1RM 总和 ÷ 体重）</view>
          <view class="f-p">男：≥4.5 → ×1.04；≥6.0 → ×1.08</view>
          <view class="f-p">女：≥3.5 → ×1.04；≥5.0 → ×1.08</view>
          <view class="f-p">未填写三大项 → ×1.00</view>
        </view>
        <view class="f-block">
          <view class="f-h">③ TDEE = 修正 BMR × 活动系数</view>
          <view class="f-p">久坐 ×1.2 ｜ 轻度活动 ×1.375</view>
          <view class="f-p">规律力量训练 ×1.55 ｜ 高强度 ×1.725</view>
        </view>
        <view class="f-block">
          <view class="f-h">④ 目标热量</view>
          <view class="f-p">增肌 TDEE+400 ｜ 减脂 TDEE−400 ｜ 维持 TDEE</view>
          <view class="f-p">自定义目标时直接取填写值</view>
        </view>
        <view class="f-block">
          <view class="f-h">⑤ 默认 PFC 比例（碳水/蛋白/脂肪）</view>
          <view class="f-p">增肌 45/32/23 ｜ 减脂 35/37/28</view>
          <view class="f-p">维持 45/28/27 ｜ 自定义 50/30/20</view>
        </view>
        <u-button type="primary" text="知道了" @click="showFormula = false"></u-button>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import dayjs from "dayjs";
import { userApi, type UserTarget } from "@/api/user";
import { useUserStore } from "@/store/user";

const GOAL_LABELS: Record<number, string> = {
  1: "增肌",
  2: "减脂",
  3: "维持",
  4: "自定义",
};

const userStore = useUserStore();
const target = ref<UserTarget | null>(null);
const showFormula = ref(false);

const info = computed(() => userStore.userInfo);

onShow(() => {
  loadAll();
});

async function loadAll(): Promise<void> {
  try {
    const [me, t] = await Promise.all([userApi.getMe(), userApi.getTarget()]);
    userStore.setUser(me);
    target.value = t;
  } catch {
    uni.showToast({ title: "加载失败", icon: "none" });
  }
}

const avatarText = computed(() => (info.value?.nickname || "微").charAt(0));

const metaText = computed(() => {
  const u = info.value;
  if (!u) return "";
  const gender = u.gender === 1 ? "男" : "女";
  const age = u.birthday ? dayjs().diff(dayjs(u.birthday), "year") : "--";
  let s = `${gender} · ${age}岁 · ${u.height ?? "--"}cm · ${u.currentWeight ?? "--"}kg`;
  if (u.targetWeight) s += ` → 目标 ${u.targetWeight}kg`;
  return s;
});

const muscleFactorText = computed(() => {
  const f = info.value?.muscleFactor;
  return f ? Number(f).toFixed(2) : "1.00";
});

const goalLabel = computed(() => GOAL_LABELS[info.value?.goalType ?? 3] ?? "维持");

const goalDeltaText = computed(() => {
  const t = target.value;
  if (!t) return "";
  const delta = Math.round(t.targetKcal - t.tdee);
  if (info.value?.goalType === 4) return "自定义热量";
  if (delta === 0) return "TDEE 持平";
  return `TDEE ${delta > 0 ? "+" : "−"}${Math.abs(delta)}`;
});

const trainingText = computed(() => {
  const u = info.value;
  if (!u) return "";
  const { squat1rm, bench1rm, deadlift1rm } = u;
  if (!squat1rm && !bench1rm && !deadlift1rm) return "未填写";
  return `蹲${squat1rm ?? "-"} 推${bench1rm ?? "-"} 拉${deadlift1rm ?? "-"}`;
});

function round(n?: number): number | string {
  return n == null ? "--" : Math.round(n);
}

function goEdit(step: number): void {
  uni.navigateTo({ url: `/pages/onboarding/index?edit=1&step=${step}` });
}

function comingSoon(): void {
  uni.showToast({ title: "v1.1 即将上线", icon: "none" });
}

function onLogout(): void {
  uni.showModal({
    title: "退出登录",
    content: "退出后需重新微信登录，确定退出吗？",
    success: (r) => {
      if (r.confirm) {
        userStore.logout();
        uni.reLaunch({ url: "/pages/onboarding/index" });
      }
    },
  });
}
</script>

<style scoped>
.mine {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}
/* 资料卡 */
.profile {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #07c160, #05a050);
  color: #fff;
  font-size: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pinfo {
  flex: 1;
}
.pname {
  font-size: 32rpx;
  font-weight: 700;
}
.pmeta {
  margin-top: 6rpx;
  font-size: 22rpx;
  color: #8a919e;
}
.arrow {
  color: #c4c9d1;
  font-size: 36rpx;
}
/* 四格 */
.grid4 {
  display: flex;
}
.cell {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}
.num {
  font-size: 32rpx;
  font-weight: 700;
  color: #1a1d23;
}
.num.primary {
  color: #05a050;
}
.label {
  font-size: 20rpx;
  color: #8a919e;
}
/* 当前目标 */
.goal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 26rpx;
  font-weight: 700;
}
.goal-delta {
  font-size: 20rpx;
  font-weight: 400;
  color: #8a919e;
}
.pfc-bar {
  display: flex;
  height: 20rpx;
  border-radius: 10rpx;
  overflow: hidden;
  margin-top: 16rpx;
}
.seg {
  height: 100%;
}
.carbo {
  background: #5b8ff9;
}
.protein {
  background: #ff7a45;
}
.fat {
  background: #f5c518;
}
.pfc-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  font-size: 20rpx;
  color: #4e5561;
}
/* 菜单 */
.menu {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}
.mi {
  display: flex;
  align-items: center;
  padding: 26rpx 24rpx;
  border-bottom: 2rpx solid #f2f3f5;
}
.mi:last-child {
  border-bottom: none;
}
.icn {
  width: 48rpx;
  font-size: 30rpx;
}
.mt {
  font-size: 28rpx;
}
.val {
  margin-left: auto;
  font-size: 20rpx;
  color: #8a919e;
}
.arr {
  margin-left: 12rpx;
  color: #c4c9d1;
  font-size: 32rpx;
}
.badge-ver {
  margin-left: auto;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
  background: #f6f0ff;
  color: #7c3aed;
  font-size: 18rpx;
}
/* 公式弹层 */
.formula {
  width: 620rpx;
  padding: 32rpx;
}
.f-title {
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
}
.f-block {
  margin-bottom: 20rpx;
}
.f-h {
  font-size: 26rpx;
  font-weight: 600;
  margin-bottom: 8rpx;
}
.f-p {
  font-size: 22rpx;
  color: #4e5561;
  line-height: 1.7;
}
</style>
