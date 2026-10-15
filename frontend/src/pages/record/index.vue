<template>
  <view class="record">
    <!-- 今日汇总：实际 vs 目标 -->
    <view class="card summary-card">
      <view class="card-title">今日汇总</view>
      <!-- TODO: 渲染 dietStore.todaySummary 实际/目标热量与 PFC -->
    </view>

    <!-- 记录列表 -->
    <view class="card list-card">
      <view class="card-title">记录明细</view>
      <!-- TODO: 渲染 dietStore.todayRecords，每条支持编辑/删除 -->
    </view>

    <!-- 添加按钮 -->
    <view class="actions">
      <u-button type="primary" @click="goAdd">+ 添加记录</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { recordApi } from "@/api/record";
import { useDietStore } from "@/store/diet";

const dietStore = useDietStore();

function goAdd(): void {
  // TODO: 弹层或独立页选择食物 + 录入克数
  // recordApi.create({ foodId, grams, mealType })
}

// TODO: onMounted 调 recordApi.todaySummary() + recordApi.getByDate(today)
</script>

<style scoped>
.record {
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
.actions {
  margin-top: 8rpx;
}
</style>
