<template>
  <view class="mine">
    <!-- 用户信息卡片 -->
    <view class="card user-card">
      <view class="card-title">我的</view>
      <!-- TODO: 渲染 userStore.userInfo（头像/昵称/目标） -->
    </view>

    <!-- 设置项列表 -->
    <view class="card settings-card">
      <view class="card-title">设置</view>
      <u-cell-group>
        <u-cell title="编辑基础信息" isLink @click="editBase"></u-cell>
        <u-cell title="训练数据" isLink @click="editTraining"></u-cell>
        <u-cell title="目标与 PFC" isLink @click="editGoal"></u-cell>
        <u-cell title="退出登录" isLink @click="logout"></u-cell>
      </u-cell-group>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from "@/store/user";

const userStore = useUserStore();

function editBase(): void {
  // TODO: 跳 onboarding 或独立编辑页
}
function editTraining(): void {
  // TODO
}
function editGoal(): void {
  // TODO
}
function logout(): void {
  userStore.logout();
  uni.reLaunch({ url: "/pages/onboarding/index" });
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
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}
</style>
