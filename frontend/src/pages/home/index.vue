<template>
  <view class="home">
    <!-- 今日目标概览：调 userApi.getTarget() -->
    <view class="card target-card">
      <view class="card-title">今日目标</view>
      <!-- TODO: 渲染 targetKcal / targetCarbo / targetProtein / targetFat -->
    </view>

    <!-- 今日摄入环形图：lime-echart 集成后替换占位 -->
    <view class="card ring-card">
      <view class="card-title">今日摄入</view>
      <view class="ring-placeholder">（图表占位：lime-echart 集成后替换）</view>
    </view>

    <!-- 今日食谱入口：跳 /pages/recipe/index -->
    <view class="card recipe-card" @click="goRecipe">
      <view class="card-title">今日食谱</view>
      <!-- TODO: 渲染 dietStore.todayRecipe 摘要 -->
    </view>

    <!-- 一键入库按钮 -->
    <view class="card record-card">
      <u-button type="primary" @click="bulkRecord">一键入库</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
function goRecipe(): void {
  uni.navigateTo({ url: "/pages/recipe/index" });
}

async function bulkRecord(): Promise<void> {
  // TODO: 接入交互
  // await recordApi.bulkToday();
}

// TODO: onMounted 时调用 userApi.getTarget() + recipeApi.getToday()
</script>

<style scoped>
.home {
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
.ring-placeholder {
  height: 200rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 24rpx;
}
</style>
