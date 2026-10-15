<template>
  <view class="recipe">
    <!-- 食谱概览：targetKcal / actualKcal -->
    <view class="card overview-card">
      <view class="card-title">今日食谱</view>
      <!-- TODO: 渲染 dietStore.todayRecipe 概览 -->
    </view>

    <!-- 餐次分组列表：早餐 / 午餐 / 晚餐 / 加餐 -->
    <view class="card items-card">
      <view class="card-title">食材明细</view>
      <!-- TODO: 按 mealType 分组渲染 recipe.items -->
      <!-- 每项卡片包含：替换食材、不喜欢按钮 -->
    </view>

    <!-- 替换弹层占位 -->
    <view class="substitute-placeholder">
      <!-- TODO: u-popup 弹替换候选列表 -->
    </view>
  </view>
</template>

<script setup lang="ts">
import { recipeApi } from "@/api/recipe";
import { useDietStore } from "@/store/diet";

const dietStore = useDietStore();

// TODO: onMounted 调用 recipeApi.getToday() 写入 dietStore
// TODO: substitute(itemId) 调 recipeApi.substitute(itemId)
// TODO: dislike(itemId) 调 recipeApi.dislike(itemId)
</script>

<style scoped>
.recipe {
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
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
</style>
