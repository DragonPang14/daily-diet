<template>
  <view class="food-page">
    <!-- 挑选模式横幅 -->
    <view v-if="dietStore.pickMode" class="pick-banner">
      挑选模式：点「＋」把食物加入今日记录
      <text class="pick-cancel" @click="cancelPick">取消</text>
    </view>

    <!-- 搜索 -->
    <view class="search">
      <text class="search-icon">🔍</text>
      <input v-model="keyword" placeholder="搜索食物，如：鸡胸肉" confirm-type="search" @input="onKeywordInput" />
      <text v-if="keyword" class="search-clear" @click="clearKeyword">✕</text>
    </view>

    <!-- 分类 -->
    <scroll-view scroll-x class="cat-scroll" :show-scrollbar="false">
      <view class="cat-row">
        <view
          v-for="c in CATEGORIES"
          :key="c"
          class="cat"
          :class="{ on: category === c }"
          @click="onCategory(c)"
        >{{ c }}</view>
      </view>
    </scroll-view>

    <view class="total-tip">共 {{ total }} 个结果</view>

    <!-- 列表 -->
    <view v-if="!loadingFirst && !list.length" class="empty">没有找到相关食物</view>
    <view v-for="f in list" :key="f.id" class="food">
      <view class="fi">
        <view class="fn">
          <text class="fn-name">{{ f.brand ? `${f.brand} ` : "" }}{{ f.name }}</text>
          <text v-if="f.source === 2" class="tag custom">自定义</text>
          <text class="tag">{{ f.category }}</text>
        </view>
        <view class="fm">
          {{ Math.round(f.energyKcal) }} kcal/100g · 蛋 {{ fmt(f.proteinG) }} · 脂 {{ fmt(f.fatG) }} · 碳 {{ fmt(f.carboG) }}
        </view>
      </view>
      <view class="add" @click="onAdd(f)">＋</view>
    </view>
    <view v-if="loadingMore" class="empty">加载中…</view>
    <view v-else-if="list.length && !hasMore" class="empty">没有更多了</view>

    <!-- 新建自定义食物 -->
    <view class="btn-custom" @click="customShow = true">✎ 新建自定义食物</view>

    <!-- 加入记录弹层 -->
    <u-popup :show="addShow" mode="bottom" :round="24" @close="addShow = false">
      <view class="pop-box" v-if="adding">
        <view class="pop-title">加入今日记录</view>
        <view class="pop-food">{{ adding.brand ? `${adding.brand} ` : "" }}{{ adding.name }}</view>
        <view class="flabel">餐次</view>
        <view class="chip-row">
          <view
            v-for="m in MEALS"
            :key="m.value"
            class="cchip"
            :class="{ on: addMealType === m.value }"
            @click="addMealType = m.value"
          >{{ m.label }}</view>
        </view>
        <view class="flabel">克数</view>
        <view class="pop-field">
          <input type="digit" v-model="addGrams" />
          <text class="unit">g · 约 {{ estKcal }} kcal</text>
        </view>
        <view class="pop-actions">
          <view class="btn-ghost" @click="addShow = false">取消</view>
          <view class="btn-primary" @click="onConfirmAdd">加入记录</view>
        </view>
      </view>
    </u-popup>

    <!-- 自定义食物表单弹层 -->
    <u-popup :show="customShow" mode="bottom" :round="24" @close="customShow = false">
      <view class="pop-box">
        <view class="pop-title">新建自定义食物</view>
        <view class="flabel">名称（品牌 + 商品名）</view>
        <view class="pop-field"><input v-model="cf.brand" placeholder="品牌（选填），如：桃李" /></view>
        <view class="pop-field" style="margin-top: 14rpx"><input v-model="cf.name" placeholder="商品名，如：全麦面包" /></view>
        <view class="flabel">类别</view>
        <view class="chip-row">
          <view
            v-for="c in CATEGORIES.slice(1)"
            :key="c"
            class="cchip"
            :class="{ on: cf.category === c }"
            @click="cf.category = c"
          >{{ c }}</view>
        </view>
        <view class="flabel">每 100g 营养成分</view>
        <view class="grid2">
          <view class="pop-field"><input type="digit" v-model="cf.energyKcal" placeholder="能量" /><text class="unit">kcal</text></view>
          <view class="pop-field"><input type="digit" v-model="cf.proteinG" placeholder="蛋白质" /><text class="unit">g</text></view>
          <view class="pop-field"><input type="digit" v-model="cf.fatG" placeholder="脂肪" /><text class="unit">g</text></view>
          <view class="pop-field"><input type="digit" v-model="cf.carboG" placeholder="碳水" /><text class="unit">g</text></view>
        </view>
        <view class="flabel">常用克数（选填）</view>
        <view class="pop-field"><input type="digit" v-model="cf.commonWeight" placeholder="如 80" /><text class="unit">g</text></view>
        <view class="pop-actions">
          <view class="btn-ghost" @click="customShow = false">取消</view>
          <view class="btn-primary" @click="onCreateCustom">保存</view>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onReachBottom, onShow } from "@dcloudio/uni-app";
import dayjs from "dayjs";
import { foodApi, type Food } from "@/api/food";
import { recordApi } from "@/api/record";
import { useDietStore } from "@/store/diet";

const dietStore = useDietStore();

const CATEGORIES = ["全部", "谷物", "蛋白", "蔬菜", "水果", "乳品", "坚果", "调料"];
const MEALS = [
  { value: 1, label: "早餐" },
  { value: 2, label: "午餐" },
  { value: 3, label: "晚餐" },
  { value: 4, label: "加餐" },
];

const keyword = ref("");
const category = ref("全部");
const list = ref<Food[]>([]);
const total = ref(0);
const page = ref(1);
const size = 20;
const loadingFirst = ref(true);
const loadingMore = ref(false);
const hasMore = computed(() => list.value.length < total.value);

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

onShow(() => {
  reload();
});

onReachBottom(() => {
  loadMore();
});

function onKeywordInput(): void {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(reload, 300);
}

function clearKeyword(): void {
  keyword.value = "";
  reload();
}

function onCategory(c: string): void {
  category.value = c;
  reload();
}

async function reload(): Promise<void> {
  page.value = 1;
  loadingFirst.value = true;
  try {
    const r = await foodApi.search({
      keyword: keyword.value || undefined,
      category: category.value === "全部" ? undefined : category.value,
      page: 1,
      size,
    });
    list.value = r.records || [];
    total.value = Number(r.total) || 0;
  } finally {
    loadingFirst.value = false;
  }
}

async function loadMore(): Promise<void> {
  if (loadingMore.value || !hasMore.value) return;
  loadingMore.value = true;
  try {
    const r = await foodApi.search({
      keyword: keyword.value || undefined,
      category: category.value === "全部" ? undefined : category.value,
      page: page.value + 1,
      size,
    });
    page.value += 1;
    list.value = [...list.value, ...(r.records || [])];
    total.value = Number(r.total) || 0;
  } finally {
    loadingMore.value = false;
  }
}

function fmt(n: number): string {
  return (Math.round(Number(n) * 10) / 10).toString();
}

// ===== 加入记录 =====
const addShow = ref(false);
const adding = ref<Food | null>(null);
const addMealType = ref(defaultMealType());
const addGrams = ref("100");

function defaultMealType(): number {
  const h = dayjs().hour();
  if (h < 10) return 1;
  if (h < 15) return 2;
  if (h < 20) return 3;
  return 4;
}

const estKcal = computed(() => {
  if (!adding.value) return 0;
  const g = Number(addGrams.value) || 0;
  return Math.round((Number(adding.value.energyKcal) * g) / 100);
});

function onAdd(f: Food): void {
  adding.value = f;
  addMealType.value = defaultMealType();
  addGrams.value = String(Math.round(Number(f.commonWeight) || 100));
  addShow.value = true;
}

async function onConfirmAdd(): Promise<void> {
  if (!adding.value) return;
  const g = Number(addGrams.value);
  if (!g || g <= 0 || g > 3000) {
    uni.showToast({ title: "克数需在 0-3000 之间", icon: "none" });
    return;
  }
  await recordApi.create({
    mealType: addMealType.value,
    foodId: adding.value.id,
    foodWeightG: g,
  });
  addShow.value = false;
  uni.showToast({ title: "已加入记录", icon: "none" });
  if (dietStore.pickMode) {
    dietStore.setPickMode(false);
    setTimeout(() => uni.switchTab({ url: "/pages/record/index" }), 400);
  }
}

function cancelPick(): void {
  dietStore.setPickMode(false);
  uni.switchTab({ url: "/pages/record/index" });
}

// ===== 自定义食物 =====
const customShow = ref(false);
const cf = reactive({
  brand: "",
  name: "",
  category: "谷物",
  energyKcal: "",
  proteinG: "",
  fatG: "",
  carboG: "",
  commonWeight: "",
});

async function onCreateCustom(): Promise<void> {
  if (!cf.name.trim()) {
    uni.showToast({ title: "请填写名称", icon: "none" });
    return;
  }
  const nums = [cf.energyKcal, cf.proteinG, cf.fatG, cf.carboG].map(Number);
  if (nums.some((n) => isNaN(n) || n < 0) || nums.some((n) => n === 0 && cf.energyKcal === "")) {
    // 能量必填，其余可为 0
  }
  if (cf.energyKcal === "" || cf.proteinG === "" || cf.fatG === "" || cf.carboG === "") {
    uni.showToast({ title: "请完整填写四项营养值", icon: "none" });
    return;
  }
  await foodApi.createCustom({
    name: cf.name.trim(),
    brand: cf.brand.trim() || undefined,
    category: cf.category,
    energyKcal: Number(cf.energyKcal),
    proteinG: Number(cf.proteinG),
    fatG: Number(cf.fatG),
    carboG: Number(cf.carboG),
    commonWeight: cf.commonWeight ? Number(cf.commonWeight) : undefined,
  });
  customShow.value = false;
  uni.showToast({ title: "已保存", icon: "none" });
  cf.brand = cf.name = cf.energyKcal = cf.proteinG = cf.fatG = cf.carboG = cf.commonWeight = "";
  reload();
}
</script>

<style scoped>
.food-page {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  min-height: 100vh;
  box-sizing: border-box;
  background: #f5f6f8;
}
.pick-banner {
  background: #e8f9f0;
  border: 1rpx solid #9fe3c2;
  color: #06a856;
  font-size: 22rpx;
  border-radius: 16rpx;
  padding: 16rpx 24rpx;
  display: flex;
  align-items: center;
}
.pick-cancel {
  margin-left: auto;
  color: #4e5561;
  border: 1rpx solid #ebedf1;
  background: #fff;
  border-radius: 8rpx;
  padding: 2rpx 14rpx;
}
.search {
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: #fff;
  border-radius: 999rpx;
  padding: 16rpx 28rpx;
  border: 1rpx solid #ebedf1;
}
.search input {
  flex: 1;
  font-size: 26rpx;
}
.search-icon {
  font-size: 24rpx;
}
.search-clear {
  color: #8a919e;
  font-size: 24rpx;
}
.cat-scroll {
  white-space: nowrap;
}
.cat-row {
  display: flex;
  gap: 14rpx;
}
.cat {
  flex: none;
  font-size: 24rpx;
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #4e5561;
  border: 1rpx solid #ebedf1;
}
.cat.on {
  background: #1f2329;
  color: #fff;
  border-color: #1f2329;
  font-weight: 600;
}
.total-tip {
  font-size: 20rpx;
  color: #8a919e;
  padding: 0 4rpx;
}
.food {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  padding: 22rpx 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(30, 40, 60, 0.04);
}
.fi {
  flex: 1;
  min-width: 0;
}
.fn {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.fn-name {
  font-size: 28rpx;
  font-weight: 600;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.tag {
  flex: none;
  font-size: 18rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  background: #f0f2f5;
  color: #8a919e;
}
.tag.custom {
  background: #fff1d6;
  color: #d48806;
}
.fm {
  font-size: 20rpx;
  color: #8a919e;
  margin-top: 8rpx;
}
.add {
  flex: none;
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: #07c160;
  color: #fff;
  font-size: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty {
  text-align: center;
  color: #8a919e;
  font-size: 22rpx;
  padding: 32rpx 0;
}
.btn-custom {
  border: 1rpx solid #ebedf1;
  background: #fff;
  color: #4e5561;
  border-radius: 20rpx;
  text-align: center;
  font-size: 26rpx;
  padding: 22rpx 0;
}
.pop-box {
  padding: 32rpx 32rpx 56rpx;
}
.pop-title {
  font-size: 32rpx;
  font-weight: 700;
}
.pop-food {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #06a856;
  font-weight: 600;
}
.flabel {
  font-size: 24rpx;
  color: #4e5561;
  font-weight: 600;
  margin: 26rpx 0 12rpx;
}
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.cchip {
  font-size: 24rpx;
  padding: 10rpx 24rpx;
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
.pop-field {
  background: #f7f8fa;
  border-radius: 16rpx;
  padding: 18rpx 24rpx;
  display: flex;
  align-items: center;
}
.pop-field input {
  flex: 1;
  font-size: 28rpx;
}
.unit {
  color: #8a919e;
  font-size: 22rpx;
}
.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}
.pop-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 36rpx;
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
