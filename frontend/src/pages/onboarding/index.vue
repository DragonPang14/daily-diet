<template>
  <view class="onboarding">
      <!-- 步骤进度（编辑模式不展示） -->
      <view v-if="!editMode" class="steps">
        <view class="step-bar" :class="{ done: step >= 1 }"></view>
        <view class="step-bar" :class="{ done: step >= 2 }"></view>
        <view class="step-bar" :class="{ done: step >= 3 }"></view>
        <text class="steps-txt">{{ step }}/3</text>
      </view>

      <!-- Step 1 基础信息 -->
      <view v-show="step === 1" class="step-box">
        <view class="h-title">{{ editMode ? "编辑基础信息" : "先认识一下你 👋" }}</view>
        <view v-if="!editMode" class="h-sub">用于 Mifflin-St Jeor 公式计算基础代谢</view>

        <view class="flabel">性别</view>
        <view class="seg2">
          <view class="opt" :class="{ on: form.gender === 1 }" @click="form.gender = 1">♂ 男</view>
          <view class="opt" :class="{ on: form.gender === 0 }" @click="form.gender = 0">♀ 女</view>
        </view>

        <view class="flabel">生日</view>
        <picker mode="date" :value="form.birthday" :end="todayStr" @change="(e: any) => (form.birthday = e.detail.value)">
          <view class="field">
            <text>{{ form.birthday || "请选择生日" }}</text>
            <text class="ph">📅</text>
          </view>
        </picker>

        <view class="row2">
          <view class="col">
            <view class="flabel">身高</view>
            <view class="field">
              <input type="digit" v-model="form.height" placeholder="如 178" />
              <text class="unit">cm</text>
            </view>
          </view>
          <view class="col">
            <view class="flabel">当前体重</view>
            <view class="field">
              <input type="digit" v-model="form.currentWeight" placeholder="如 75.0" />
              <text class="unit">kg</text>
            </view>
          </view>
        </view>

        <view class="flabel">目标体重（选填）</view>
        <view class="field">
          <input type="digit" v-model="form.targetWeight" placeholder="如 70.0" />
          <text class="unit">kg</text>
        </view>
      </view>

      <!-- Step 2 训练水平 -->
      <view v-show="step === 2" class="step-box">
        <view class="h-title">{{ editMode ? "编辑训练数据" : "你的力量水平 💪" }}</view>
        <view v-if="!editMode" class="h-sub">三大项 1RM 用于估算肌肉量修正 BMR；不填则按普通人群计算</view>

        <view class="row3">
          <view class="col">
            <view class="flabel">深蹲 1RM</view>
            <view class="field"><input type="digit" v-model="form.squat1rm" placeholder="kg" /></view>
          </view>
          <view class="col">
            <view class="flabel">卧推 1RM</view>
            <view class="field"><input type="digit" v-model="form.bench1rm" placeholder="kg" /></view>
          </view>
          <view class="col">
            <view class="flabel">硬拉 1RM</view>
            <view class="field"><input type="digit" v-model="form.deadlift1rm" placeholder="kg" /></view>
          </view>
        </view>

        <view class="flabel">每周训练频率（活动系数）</view>
        <view class="chip-row">
          <view
            v-for="lv in LEVELS"
            :key="lv.value"
            class="cchip"
            :class="{ on: form.trainingLevel === lv.value }"
            @click="form.trainingLevel = form.trainingLevel === lv.value ? 0 : lv.value"
          >{{ lv.label }}</view>
        </view>
      </view>

      <!-- Step 3 目标设置 -->
      <view v-show="step === 3" class="step-box">
        <view class="h-title">{{ editMode ? "编辑目标" : "选择你的目标 🎯" }}</view>

        <view class="goal-grid">
          <view
            v-for="g in GOALS"
            :key="g.value"
            class="cchip goal"
            :class="{ on: form.goalType === g.value }"
            @click="form.goalType = g.value"
          >
            <text class="goal-name">{{ g.name }}</text>
            <text class="goal-desc">{{ g.desc }}</text>
          </view>
        </view>

        <view v-if="form.goalType === 4">
          <view class="flabel">自定义每日目标热量</view>
          <view class="field">
            <input type="number" v-model="form.customTdee" placeholder="如 2000" />
            <text class="unit">kcal</text>
          </view>
        </view>

        <!-- 计算结果卡 -->
        <view v-if="target" class="result-card">
          <view class="result-label">你的每日目标热量</view>
          <view class="result-kcal">{{ target.targetKcal }} <text class="result-unit">kcal</text></view>
          <view class="result-chain">
            BMR {{ Math.round(target.bmr) }} <template v-if="muscleFactor > 1">→ 肌肉修正 ×{{ muscleFactor }}</template>
            → TDEE {{ Math.round(target.tdee) }}
            <template v-if="form.goalType !== 3 && form.goalType !== 4">→ 目标偏移 → {{ target.targetKcal }}</template>
          </view>
        </view>

        <view v-if="target" class="pfc-card">
          <view class="pfc-title">PFC 比例<text class="pfc-hint">（可在「我的」中修改）</text></view>
          <view class="pfc-bar">
            <view class="seg carbo" :style="{ width: target.pfcCarboPct + '%' }"></view>
            <view class="seg protein" :style="{ width: target.pfcProteinPct + '%' }"></view>
            <view class="seg fat" :style="{ width: target.pfcFatPct + '%' }"></view>
          </view>
          <view class="pfc-nums">
            <text>碳水 {{ target.pfcCarboPct }}% · {{ Math.round(target.targetCarboG) }}g</text>
            <text>蛋白 {{ target.pfcProteinPct }}% · {{ Math.round(target.targetProteinG) }}g</text>
            <text>脂肪 {{ target.pfcFatPct }}% · {{ Math.round(target.targetFatG) }}g</text>
          </view>
        </view>
      </view>

      <!-- 底部操作 -->
      <view class="actions">
        <view v-if="!editMode && step > 1" class="btn-ghost" @click="step--">上一步</view>
        <view v-if="step < 3" class="btn-primary flex1" @click="onNext">下一步</view>
        <template v-else>
          <view v-if="!target" class="btn-primary flex1" @click="onSaveGoal">查看我的目标</view>
          <view v-else class="btn-primary flex1" @click="onFinish">
            {{ editMode ? "完成" : "✨ 生成我的今日食谱" }}
          </view>
        </template>
      </view>
      <view v-if="!editMode && step === 2" class="skip" @click="skipTraining">跳过，按普通人群计算 ›</view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import dayjs from "dayjs";
import { userApi, type UserTarget } from "@/api/user";
import { recipeApi } from "@/api/recipe";
import { useUserStore } from "@/store/user";
import { today as todayStr } from "@/utils/format";

const userStore = useUserStore();

const editMode = ref(false);
const step = ref(1);
const target = ref<UserTarget | null>(null);
const muscleFactor = ref(1);
const saving = ref(false);

const LEVELS = [
  { value: 1, label: "久坐 ×1.2" },
  { value: 2, label: "1–3 次 ×1.375" },
  { value: 3, label: "3–5 次 ×1.55" },
  { value: 4, label: "6–7 次 ×1.725" },
];

const GOALS = [
  { value: 1, name: "增肌", desc: "+400 kcal" },
  { value: 2, name: "减脂", desc: "−400 kcal" },
  { value: 3, name: "维持", desc: "TDEE 持平" },
  { value: 4, name: "自定义", desc: "手动填 kcal" },
];

const form = reactive({
  gender: 1,
  birthday: "1998-01-01",
  height: "",
  currentWeight: "",
  targetWeight: "",
  squat1rm: "",
  bench1rm: "",
  deadlift1rm: "",
  trainingLevel: 0,
  goalType: 2,
  customTdee: "",
});

onLoad(async (q) => {
  // App.vue 已完成静默登录并校验 isNewUser，此处一定是已登录新用户
  // 编辑模式：从「我的」进入，跳过登录流程
  if (q?.edit === "1") {
    editMode.value = true;
    step.value = Math.min(Math.max(Number(q.step) || 1, 1), 3);
    await ensureUserInfo();
    prefill();
    if (editMode.value && step.value === 3) {
      // 编辑目标时直接展示当前计算结果
      await refreshTarget();
    }
  }
});

function goHome(): void {
  uni.switchTab({ url: "/pages/home/index" });
}

async function ensureUserInfo(): Promise<void> {
  if (!userStore.userInfo) {
    userStore.setUser(await userApi.getMe());
  }
}

function prefill(): void {
  const u = userStore.userInfo;
  if (!u) return;
  form.gender = u.gender ?? 1;
  form.birthday = u.birthday || form.birthday;
  form.height = u.height ? String(u.height) : "";
  form.currentWeight = u.currentWeight ? String(u.currentWeight) : "";
  form.targetWeight = u.targetWeight ? String(u.targetWeight) : "";
  form.squat1rm = u.squat1rm ? String(u.squat1rm) : "";
  form.bench1rm = u.bench1rm ? String(u.bench1rm) : "";
  form.deadlift1rm = u.deadlift1rm ? String(u.deadlift1rm) : "";
  form.trainingLevel = u.trainingLevel || 0;
  form.goalType = u.goalType || 2;
  form.customTdee = u.customTdee ? String(u.customTdee) : "";
}

function toast(msg: string): void {
  uni.showToast({ title: msg, icon: "none" });
}

function validStep1(): boolean {
  const age = dayjs().diff(dayjs(form.birthday), "year");
  if (!form.birthday || age < 14 || age > 80) {
    toast("年龄需在 14-80 岁之间");
    return false;
  }
  const h = Number(form.height);
  if (!h || h < 120 || h > 230) {
    toast("身高需在 120-230 cm 之间");
    return false;
  }
  const w = Number(form.currentWeight);
  if (!w || w < 30 || w > 200) {
    toast("体重需在 30-200 kg 之间");
    return false;
  }
  const tw = form.targetWeight ? Number(form.targetWeight) : null;
  if (tw !== null && (tw < 30 || tw > 200)) {
    toast("目标体重需在 30-200 kg 之间");
    return false;
  }
  return true;
}

async function saveStep1(): Promise<boolean> {
  if (!validStep1()) return false;
  const u = await userApi.updateMe({
    gender: form.gender,
    birthday: form.birthday,
    height: Number(form.height),
    currentWeight: Number(form.currentWeight),
    targetWeight: form.targetWeight ? Number(form.targetWeight) : undefined,
  });
  userStore.setUser(u);
  return true;
}

function hasTrainingInput(): boolean {
  return Boolean(
    form.squat1rm || form.bench1rm || form.deadlift1rm || form.trainingLevel > 0
  );
}

async function saveStep2(): Promise<boolean> {
  if (!hasTrainingInput()) return true; // 全空：后端按普通人群
  const u = await userApi.updateTraining({
    squat1rm: form.squat1rm ? Number(form.squat1rm) : undefined,
    bench1rm: form.bench1rm ? Number(form.bench1rm) : undefined,
    deadlift1rm: form.deadlift1rm ? Number(form.deadlift1rm) : undefined,
    trainingLevel: form.trainingLevel > 0 ? form.trainingLevel : undefined,
  });
  userStore.setUser(u);
  return true;
}

function validStep3(): boolean {
  if (form.goalType === 4) {
    const c = Number(form.customTdee);
    if (!c || c < 800 || c > 6000) {
      toast("自定义热量需在 800-6000 kcal 之间");
      return false;
    }
  }
  return true;
}

async function saveStep3(): Promise<boolean> {
  if (!validStep3()) return false;
  const u = await userApi.updateGoal({
    goalType: form.goalType,
    customTdee: form.goalType === 4 ? Number(form.customTdee) : undefined,
  });
  userStore.setUser(u);
  muscleFactor.value = Number(u.muscleFactor) || 1;
  await refreshTarget();
  return true;
}

async function refreshTarget(): Promise<void> {
  target.value = await userApi.getTarget();
  if (!editMode.value || !userStore.userInfo) {
    const u = await userApi.getMe();
    userStore.setUser(u);
    muscleFactor.value = Number(u.muscleFactor) || 1;
  } else {
    muscleFactor.value = Number(userStore.userInfo.muscleFactor) || 1;
  }
}

/** 下一步 / 编辑保存 */
async function onNext(): Promise<void> {
  if (saving.value) return;
  saving.value = true;
  try {
    const ok = step.value === 1 ? await saveStep1() : await saveStep2();
    if (!ok) return;
    if (editMode.value) {
      toast("已保存");
      uni.navigateBack();
      return;
    }
    step.value++;
  } finally {
    saving.value = false;
  }
}

function skipTraining(): void {
  step.value = 3;
}

/** Step 3：保存目标并展示计算结果 */
async function onSaveGoal(): Promise<void> {
  if (saving.value) return;
  saving.value = true;
  try {
    await saveStep3();
  } finally {
    saving.value = false;
  }
}

/** 完成：编辑模式返回；新手触发生成今日食谱后进入首页 */
async function onFinish(): Promise<void> {
  if (editMode.value) {
    uni.navigateBack();
    return;
  }
  try {
    await recipeApi.generate();
  } catch (e) {
    // 生成失败不阻塞，首页会再次拉取（后端自动生成）
  }
  goHome();
}
</script>

<style scoped>
.onboarding {
  padding: 40rpx 32rpx;
  min-height: 100vh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}
.center-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.loading-txt {
  color: #8a919e;
  font-size: 26rpx;
}
.retry {
  margin-top: 32rpx;
  width: 240rpx;
}
.steps {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.step-bar {
  flex: 1;
  height: 8rpx;
  border-radius: 4rpx;
  background: #e3e6eb;
}
.step-bar.done {
  background: #07c160;
}
.steps-txt {
  font-size: 20rpx;
  color: #8a919e;
}
.step-box {
  margin-top: 24rpx;
  flex: 1;
}
.h-title {
  font-size: 42rpx;
  font-weight: 700;
}
.h-sub {
  font-size: 22rpx;
  color: #8a919e;
  margin-top: 8rpx;
  line-height: 1.7;
}
.flabel {
  font-size: 24rpx;
  color: #4e5561;
  font-weight: 600;
  margin: 28rpx 0 12rpx;
}
.seg2 {
  display: flex;
  gap: 20rpx;
}
.opt {
  flex: 1;
  border: 3rpx solid #ebedf1;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx 0;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
  color: #4e5561;
}
.opt.on {
  border-color: #07c160;
  background: #e8f9f0;
  color: #06a856;
}
.field {
  background: #fff;
  border: 1rpx solid #ebedf1;
  border-radius: 18rpx;
  padding: 20rpx 24rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 28rpx;
}
.field input {
  flex: 1;
  font-size: 28rpx;
}
.ph {
  color: #8a919e;
}
.unit {
  color: #8a919e;
  font-size: 22rpx;
  margin-left: 12rpx;
}
.row2 {
  display: flex;
  gap: 20rpx;
}
.row3 {
  display: flex;
  gap: 16rpx;
}
.col {
  flex: 1;
}
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.cchip {
  font-size: 24rpx;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: #fff;
  border: 1rpx solid #ebedf1;
  color: #4e5561;
}
.cchip.on {
  background: #e8f9f0;
  border-color: #07c160;
  color: #06a856;
  font-weight: 600;
}
.goal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 24rpx;
}
.goal {
  border-radius: 20rpx;
  padding: 24rpx 0;
  text-align: center;
  display: flex;
  flex-direction: column;
}
.goal-name {
  font-size: 30rpx;
  font-weight: 700;
}
.goal-desc {
  font-size: 20rpx;
  color: #8a919e;
  margin-top: 6rpx;
}
.result-card {
  margin-top: 32rpx;
  background: linear-gradient(135deg, #0e9f55, #07c160);
  color: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
}
.result-label {
  font-size: 22rpx;
  opacity: 0.85;
}
.result-kcal {
  font-size: 60rpx;
  font-weight: 800;
  margin: 8rpx 0 12rpx;
}
.result-unit {
  font-size: 24rpx;
  font-weight: 400;
}
.result-chain {
  font-size: 20rpx;
  opacity: 0.92;
  line-height: 1.8;
}
.pfc-card {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx 28rpx;
}
.pfc-title {
  font-size: 24rpx;
  font-weight: 700;
}
.pfc-hint {
  font-weight: 400;
  font-size: 20rpx;
  color: #8a919e;
  margin-left: 8rpx;
}
.pfc-bar {
  display: flex;
  height: 12rpx;
  border-radius: 6rpx;
  overflow: hidden;
  margin-top: 18rpx;
}
.seg.carbo {
  background: #5b8ff9;
}
.seg.protein {
  background: #ff7a45;
}
.seg.fat {
  background: #f5c518;
}
.pfc-nums {
  display: flex;
  justify-content: space-between;
  font-size: 20rpx;
  color: #4e5561;
  margin-top: 14rpx;
}
.actions {
  display: flex;
  gap: 20rpx;
  align-items: center;
  margin-top: 40rpx;
}
.flex1 {
  flex: 1;
}
.btn-primary {
  background: #07c160;
  color: #fff;
  border-radius: 20rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
  padding: 24rpx 0;
  box-shadow: 0 12rpx 28rpx rgba(7, 193, 96, 0.28);
}
.btn-ghost {
  width: 180rpx;
  border: 1rpx solid #ebedf1;
  color: #4e5561;
  border-radius: 20rpx;
  text-align: center;
  font-size: 26rpx;
  padding: 22rpx 0;
  background: #fff;
}
.skip {
  text-align: center;
  font-size: 22rpx;
  color: #8a919e;
  padding: 24rpx 0 8rpx;
}
</style>
