<template>
  <view class="food">
    <!-- 搜索栏：keyword + 分类筛选 -->
    <view class="search-bar">
      <u-search v-model="keyword" placeholder="搜索食物" @search="onSearch"></u-search>
    </view>

    <!-- 分类筛选 -->
    <view class="category-bar">
      <!-- TODO: 渲染分类标签，点击切换 category 过滤 -->
    </view>

    <!-- 食物列表 -->
    <view class="food-list">
      <!-- TODO: 渲染 foodApi.search() 返回的 PageResult<Food> -->
    </view>

    <!-- 自定义食物入口 -->
    <view class="custom-entry">
      <u-button type="primary" @click="goCreate">+ 自定义食物</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { foodApi } from "@/api/food";

const keyword = ref("");
const category = ref("");

function onSearch(): void {
  // TODO: 调 foodApi.search({ keyword, category, page: 1 })
}

function goCreate(): void {
  // TODO: 跳自定义食物创建页（暂未独立路由，可弹层）
}
</script>

<style scoped>
.food {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}
.search-bar,
.category-bar,
.food-list,
.custom-entry {
  background: #fff;
  border-radius: 16rpx;
  padding: 16rpx 24rpx;
}
</style>
