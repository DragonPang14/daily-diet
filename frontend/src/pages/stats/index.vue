<template>
  <view class="stats">
    <!-- 周/月切换 -->
    <u-subsection
      :list="RANGE_TABS"
      :current="rangeIndex"
      active-color="#07c160"
      @change="onRangeChange"
    ></u-subsection>

    <!-- 达成率卡片 -->
    <view class="card achv-card">
      <view class="cell">
        <text class="num primary">{{ achievementText }}</text>
        <text class="label">目标达成率</text>
      </view>
      <view class="cell">
        <text class="num">{{ activeDaysText }}</text>
        <text class="label">达标天数</text>
      </view>
      <view class="cell">
        <text class="num">{{ avgKcal }}</text>
        <text class="label">日均摄入 kcal</text>
      </view>
    </view>

    <!-- 每日摄入折线（目标虚线） -->
    <view class="card">
      <view class="card-title">
        每日摄入 vs 目标
        <text v-if="targetKcal" class="hint">虚线 = 目标 {{ targetKcal }}</text>
      </view>
      <TrendLine :values="kcalValues" :labels="xLabels" :target="targetKcal" />
    </view>

    <!-- 每日 PFC 堆叠柱 -->
    <view class="card">
      <view class="card-title">每日 PFC 结构 <text class="hint">柱状堆叠（g）</text></view>
      <PfcStackBars :data="pfcData" />
      <view class="x-labels" v-if="xLabels.length">
        <text
          v-for="(lb, i) in xLabels"
          :key="i"
          class="x-label"
          :style="{ width: 100 / xLabels.length + '%' }"
        >{{ lb }}</text>
      </view>
    </view>

    <!-- AI 建议占位（v1.2） -->
    <view class="card ai-card">
      <view class="ai-title">🤖 AI 饮食建议 <text class="badge-v12">v1.2</text></view>
      <view class="ai-desc">基于你的打卡与营养数据，生成个性化饮食调整建议，v1.2 即将上线，敬请期待。</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import dayjs from "dayjs";
import { statsApi, type Achievement, type DailyStats } from "@/api/stats";
import { userApi } from "@/api/user";
import { daysAgo, formatPct, today } from "@/utils/format";
import TrendLine from "@/components/TrendLine.vue";
import PfcStackBars, { type PfcDay } from "@/components/PfcStackBars.vue";

const RANGE_TABS = ["近 7 天", "近 30 天"];
/** tab 索引对应统计天数 */
const RANGE_DAYS = [7, 30];
const WEEKDAYS = ["日", "一", "二", "三", "四", "五", "六"];

const rangeIndex = ref(0);
const loading = ref(false);
const list = ref<DailyStats[]>([]);
const achv = ref<Achievement | null>(null);
const targetKcal = ref(0);

onShow(() => {
  if (!targetKcal.value) loadTarget();
  loadData();
});

async function loadTarget(): Promise<void> {
  try {
    const t = await userApi.getTarget();
    targetKcal.value = t.targetKcal;
  } catch {
    // 目标获取失败不阻塞趋势展示
  }
}

async function loadData(): Promise<void> {
  const days = RANGE_DAYS[rangeIndex.value];
  loading.value = true;
  try {
    const [w, a] = await Promise.all([
      statsApi.weekly(daysAgo(days - 1)),
      statsApi.achievement(days),
    ]);
    list.value = w;
    achv.value = a;
  } catch {
    uni.showToast({ title: "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function onRangeChange(i: number): void {
  rangeIndex.value = i;
  loadData();
}

/** 将后端返回的稀疏日数据补齐为连续日期序列（缺失日按 0 处理） */
const days = computed(() => {
  const n = RANGE_DAYS[rangeIndex.value];
  const byDate = new Map(list.value.map((d) => [d.date, d]));
  const end = today();
  const out: { date: string; stats: DailyStats }[] = [];
  for (let i = n - 1; i >= 0; i--) {
    const date = daysAgo(i);
    const s = byDate.get(date);
    out.push({
      date,
      stats: s ?? { date, kcal: 0, proteinG: 0, fatG: 0, carboG: 0 },
    });
  }
  return out;
});

const kcalValues = computed(() => days.value.map((d) => d.stats.kcal));

/** x 轴标签：7 天显示星期；30 天每隔 5 天显示日号，避免拥挤 */
const xLabels = computed(() => {
  const isWeek = RANGE_DAYS[rangeIndex.value] === 7;
  const n = days.value.length;
  return days.value.map((d, i) => {
    const dj = dayjs(d.date);
    if (isWeek) return WEEKDAYS[dj.day()];
    return i % 5 === 0 || i === n - 1 ? String(dj.date()) : "";
  });
});

const pfcData = computed<PfcDay[]>(() =>
  days.value.map((d, i) => ({
    label: xLabels.value[i],
    carbo: d.stats.carboG,
    protein: d.stats.proteinG,
    fat: d.stats.fatG,
  }))
);

const achievementText = computed(() =>
  achv.value ? formatPct(achv.value.achievementRate) : "--"
);

const activeDaysText = computed(() =>
  achv.value ? `${achv.value.activeDays} / ${achv.value.totalDays}` : "--"
);

/** 日均摄入：仅统计有记录的天，避免无记录日拉低均值 */
const avgKcal = computed(() => {
  const recorded = list.value.filter((d) => d.kcal > 0);
  if (!recorded.length) return 0;
  const sum = recorded.reduce((acc, d) => acc + d.kcal, 0);
  return Math.round(sum / recorded.length);
});
</script>

<style scoped>
.stats {
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
.card-title {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
.hint {
  margin-left: 12rpx;
  font-size: 20rpx;
  font-weight: 400;
  color: #8a919e;
}
/* 达成率三格 */
.achv-card {
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
  font-size: 36rpx;
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
/* PFC 图 x 轴 */
.x-labels {
  display: flex;
  margin-top: 8rpx;
}
.x-label {
  text-align: center;
  font-size: 18rpx;
  color: #8a919e;
}
/* AI 占位卡 */
.ai-card {
  background: linear-gradient(135deg, #f6f0ff, #efe6ff);
  border: 2rpx solid #e4d6ff;
}
.ai-title {
  font-size: 26rpx;
  font-weight: 700;
}
.badge-v12 {
  margin-left: 8rpx;
  padding: 2rpx 10rpx;
  border-radius: 8rpx;
  background: #7c3aed;
  color: #fff;
  font-size: 18rpx;
  vertical-align: middle;
}
.ai-desc {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #4e5561;
  line-height: 1.7;
}
</style>
