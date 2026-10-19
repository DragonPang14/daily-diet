<template>
  <view class="record-page">
    <!-- 汇总卡 -->
    <view class="card">
      <view class="ring-wrap">
        <NutriRing
          :percent="ringPercent"
          :center-text="String(Math.round(actualKcal))"
          sub-text="/ 目标 kcal"
          :size="220"
          :thickness="20"
        />
        <view class="nums">
          <PfcBars
            :carbo-actual="summary?.totalCarboG || 0"
            :carbo-target="summary?.targetCarboG || 0"
            :protein-actual="summary?.totalProteinG || 0"
            :protein-target="summary?.targetProteinG || 0"
            :fat-actual="summary?.totalFatG || 0"
            :fat-target="summary?.targetFatG || 0"
          />
          <view class="hint">目标 {{ Math.round(targetKcal) }} kcal · 还差 {{ Math.round(remainKcal) }} kcal</view>
        </view>
      </view>
    </view>

    <!-- 按餐分组明细 -->
    <view v-if="loading" class="loading">加载中…</view>
    <template v-else>
      <view v-for="g in groups" :key="g.mealType" class="card meal">
        <view class="meal-head">
          <text class="meal-tag" :class="g.cls">{{ g.name }}</text>
          <text class="meal-kcal"><text class="meal-kcal-num">{{ Math.round(g.kcal) }}</text> kcal</text>
        </view>
        <view class="rec-item" v-for="r in g.records" :key="r.id">
          <text class="rn">{{ dietStore.foodName(r.foodId) }}</text>
          <text class="rg">{{ Math.round(r.foodWeightG) }}g</text>
          <text class="rk">{{ Math.round(r.energyKcal) }} kcal</text>
          <view class="ops">
            <text class="op" @click="onEdit(r)">改</text>
            <text class="op del" @click="onDelete(r)">删</text>
          </view>
        </view>
      </view>
      <view v-if="!groups.length" class="loading">今天还没有饮食记录</view>
    </template>

    <view class="btn-add" @click="goPick">＋ 手动添加食物</view>

    <!-- 修改克数弹层 -->
    <u-popup :show="editShow" mode="bottom" :round="24" @close="editShow = false">
      <view class="edit-box">
        <view class="edit-title">修改克数</view>
        <view class="edit-name" v-if="editing">{{ dietStore.foodName(editing.foodId) }}</view>
        <view class="edit-field">
          <input type="digit" v-model="editGrams" focus />
          <text class="unit">g</text>
        </view>
        <view class="edit-actions">
          <view class="btn-ghost" @click="editShow = false">取消</view>
          <view class="btn-primary" @click="onSaveEdit">保存</view>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import NutriRing from "@/components/NutriRing.vue";
import PfcBars from "@/components/PfcBars.vue";
import { recordApi, type DietRecord } from "@/api/record";
import { useDietStore } from "@/store/diet";
import { clamp } from "@/utils/calc";

const dietStore = useDietStore();
const loading = ref(true);
const editShow = ref(false);
const editing = ref<DietRecord | null>(null);
const editGrams = ref("");

const summary = computed(() => dietStore.todaySummary);
const actualKcal = computed(() => Number(summary.value?.totalKcal) || 0);
const targetKcal = computed(() => Number(summary.value?.targetKcal) || 0);
const remainKcal = computed(() => Math.max(targetKcal.value - actualKcal.value, 0));
const ringPercent = computed(() =>
  targetKcal.value > 0 ? clamp(actualKcal.value / targetKcal.value, 0, 1) : 0
);

const MEAL_MAP: Record<number, { name: string; cls: string }> = {
  1: { name: "早餐", cls: "t-breakfast" },
  2: { name: "午餐", cls: "t-lunch" },
  3: { name: "晚餐", cls: "t-dinner" },
  4: { name: "加餐", cls: "t-snack" },
};

interface Group {
  mealType: number;
  name: string;
  cls: string;
  kcal: number;
  records: DietRecord[];
}

const groups = computed<Group[]>(() => {
  const records = summary.value?.records || [];
  return [1, 2, 3, 4]
    .map((mt) => {
      const rs = records.filter((r) => r.mealType === mt);
      return {
        mealType: mt,
        name: MEAL_MAP[mt].name,
        cls: MEAL_MAP[mt].cls,
        kcal: rs.reduce((acc, r) => acc + (Number(r.energyKcal) || 0), 0),
        records: rs,
      };
    })
    .filter((g) => g.records.length > 0);
});

onShow(() => {
  loadAll();
});

async function loadAll(): Promise<void> {
  loading.value = true;
  try {
    const sum = await recordApi.todaySummary();
    dietStore.setSummary(sum);
    await dietStore.ensureFoodNames((sum.records || []).map((r) => r.foodId));
  } finally {
    loading.value = false;
  }
}

async function refresh(): Promise<void> {
  dietStore.setSummary(await recordApi.todaySummary());
}

function onEdit(r: DietRecord): void {
  editing.value = r;
  editGrams.value = String(Math.round(r.foodWeightG));
  editShow.value = true;
}

async function onSaveEdit(): Promise<void> {
  const g = Number(editGrams.value);
  if (!editing.value) return;
  if (!g || g <= 0 || g > 3000) {
    uni.showToast({ title: "克数需在 0-3000 之间", icon: "none" });
    return;
  }
  const r = editing.value;
  await recordApi.update(r.id, {
    mealType: r.mealType,
    foodId: r.foodId,
    foodWeightG: g,
  });
  editShow.value = false;
  await refresh();
  uni.showToast({ title: "已保存", icon: "none" });
}

function onDelete(r: DietRecord): void {
  uni.showModal({
    title: "删除记录",
    content: `确定删除「${dietStore.foodName(r.foodId)}」吗？`,
    confirmColor: "#f5222d",
    success: async (res) => {
      if (!res.confirm) return;
      await recordApi.remove(r.id);
      await refresh();
      uni.showToast({ title: "已删除", icon: "none" });
    },
  });
}

function goPick(): void {
  dietStore.setPickMode(true);
  uni.switchTab({ url: "/pages/food/index" });
}
</script>

<style scoped>
.record-page {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  min-height: 100vh;
  box-sizing: border-box;
  background: #f5f6f8;
}
.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 26rpx;
  box-shadow: 0 4rpx 20rpx rgba(30, 40, 60, 0.05);
}
.ring-wrap {
  display: flex;
  align-items: center;
  gap: 28rpx;
}
.nums {
  flex: 1;
}
.hint {
  margin-top: 16rpx;
  font-size: 20rpx;
  color: #8a919e;
}
.meal-head {
  display: flex;
  align-items: center;
  margin-bottom: 8rpx;
}
.meal-tag {
  font-size: 22rpx;
  font-weight: 700;
  padding: 4rpx 16rpx;
  border-radius: 10rpx;
}
.t-breakfast {
  background: #fff4e5;
  color: #d48806;
}
.t-lunch {
  background: #e8f9f0;
  color: #06a856;
}
.t-dinner {
  background: #e8f0fe;
  color: #3b6fe0;
}
.t-snack {
  background: #f3e8ff;
  color: #7b3fe4;
}
.meal-kcal {
  margin-left: auto;
  font-size: 22rpx;
  color: #4e5561;
}
.meal-kcal-num {
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2329;
}
.rec-item {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
  border-top: 1rpx dashed #f0f1f4;
}
.rn {
  flex: 1;
  font-size: 26rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.rg {
  width: 110rpx;
  text-align: right;
  font-size: 22rpx;
  color: #8a919e;
}
.rk {
  width: 130rpx;
  text-align: right;
  font-size: 22rpx;
  color: #4e5561;
}
.ops {
  display: flex;
  gap: 10rpx;
  margin-left: 12rpx;
}
.op {
  font-size: 20rpx;
  color: #4e5561;
  border: 1rpx solid #ebedf1;
  border-radius: 8rpx;
  padding: 2rpx 12rpx;
}
.op.del {
  color: #f5222d;
  border-color: #ffd6d6;
}
.loading {
  text-align: center;
  color: #8a919e;
  font-size: 24rpx;
  padding: 48rpx 0;
}
.btn-add {
  border: 1rpx solid #ebedf1;
  background: #fff;
  color: #4e5561;
  border-radius: 20rpx;
  text-align: center;
  font-size: 26rpx;
  padding: 22rpx 0;
}
.edit-box {
  padding: 32rpx 32rpx 48rpx;
}
.edit-title {
  font-size: 32rpx;
  font-weight: 700;
}
.edit-name {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #8a919e;
}
.edit-field {
  margin-top: 24rpx;
  background: #f7f8fa;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  display: flex;
  align-items: center;
}
.edit-field input {
  flex: 1;
  font-size: 32rpx;
}
.unit {
  color: #8a919e;
  font-size: 24rpx;
}
.edit-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
}
.btn-ghost {
  flex: 1;
  border: 1rpx solid #ebedf1;
  color: #4e5561;
  border-radius: 16rpx;
  text-align: center;
  font-size: 28rpx;
  padding: 20rpx 0;
  background: #fff;
}
.btn-primary {
  flex: 1;
  background: #07c160;
  color: #fff;
  border-radius: 16rpx;
  text-align: center;
  font-size: 28rpx;
  font-weight: 600;
  padding: 20rpx 0;
}
</style>
