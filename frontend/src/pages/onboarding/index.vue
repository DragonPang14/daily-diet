<template>
  <view class="onboarding">
    <!-- 步骤 1：基础信息（性别 / 出生日期 / 身高 / 体重） -->
    <view class="step step-1" v-if="step === 1">
      <view class="step-title">基础信息</view>
      <!-- TODO: uview-plus 表单组件接入后补全字段 -->
    </view>

    <!-- 步骤 2：训练数据（三大项 + 训练等级） -->
    <view class="step step-2" v-else-if="step === 2">
      <view class="step-title">训练数据</view>
      <!-- TODO: squat / bench / deadlift / trainingLevel 表单 -->
    </view>

    <!-- 步骤 3：目标设定（goalType + PFC 比例） -->
    <view class="step step-3" v-else>
      <view class="step-title">目标设定</view>
      <!-- TODO: goalType 选择器 + PFC 滑块 -->
    </view>

    <view class="actions">
      <u-button v-if="step > 1" @click="prev">上一步</u-button>
      <u-button type="primary" @click="next">{{ step < 3 ? "下一步" : "完成" }}</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";

const step = ref(1);

function next(): void {
  if (step.value < 3) {
    step.value++;
  } else {
    // TODO: 收集三步数据后调用 userApi.updateMe / updateTraining / updateGoal
    // 完成后跳转 home
    uni.switchTab({ url: "/pages/home/index" });
  }
}

function prev(): void {
  if (step.value > 1) step.value--;
}
</script>

<style scoped>
.onboarding {
  padding: 32rpx;
  min-height: 100vh;
}
.step-title {
  font-size: 36rpx;
  font-weight: 600;
  margin-bottom: 24rpx;
}
.actions {
  margin-top: 48rpx;
  display: flex;
  gap: 24rpx;
}
</style>
