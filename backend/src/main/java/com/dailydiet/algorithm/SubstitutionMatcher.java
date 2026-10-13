package com.dailydiet.algorithm;

import com.dailydiet.food.entity.Food;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 食材替换匹配器 — 余弦相似度 + 热量接近度 + 不喜欢惩罚
 */
@Component
public class SubstitutionMatcher {

    private static final double ALPHA = 0.6;  // PFC 相似度权重
    private static final double BETA  = 0.4;  // 热量接近度权重
    private static final double GAMMA = 100;  // 不喜欢惩罚（大值 → 直接排除）

    /**
     * 对候选食物列表按替换匹配度打分排序
     *
     * @param original       原食材
     * @param candidates     候选列表（已通过类别+热量±20%预过滤）
     * @param dislikeCount   每个候选的累计 dislike 次数（0 表示未标记）
     * @return 按分数降序排列的候选列表
     */
    public List<Food> rankCandidates(Food original, List<Food> candidates,
                                      Map<Long, Integer> dislikeCount) {
        PfcVector origPfc = new PfcVector(
                original.getCarboG().doubleValue(),
                original.getProteinG().doubleValue(),
                original.getFatG().doubleValue()
        );
        double origKcal = original.getEnergyKcal().doubleValue();

        return candidates.stream()
                .sorted(Comparator.comparingDouble((Food c) -> {
                    PfcVector cPfc = new PfcVector(
                            c.getCarboG().doubleValue(),
                            c.getProteinG().doubleValue(),
                            c.getFatG().doubleValue()
                    );
                    double cosine = origPfc.cosineSimilarity(cPfc);
                    double cKcal = c.getEnergyKcal().doubleValue();
                    double calClose = 1 - Math.abs(cKcal - origKcal) / Math.max(cKcal, origKcal);
                    int dislike = dislikeCount.getOrDefault(c.getId(), 0);
                    double penalty = dislike > 0 ? GAMMA : 0;
                    return -(ALPHA * cosine + BETA * calClose - penalty);
                }))
                .collect(Collectors.toList());
    }
}
