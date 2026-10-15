package com.dailydiet.stats.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dailydiet.record.entity.DietRecord;
import com.dailydiet.record.mapper.DietRecordMapper;
import com.dailydiet.stats.vo.AchievementVO;
import com.dailydiet.stats.vo.WeeklyStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统计服务 —— 营养摄入统计、打卡达成率
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final DietRecordMapper recordMapper;

    /**
     * 区间内每日营养摄入统计（按日期聚合）
     */
    public List<WeeklyStatsVO> getDailyStats(Long userId, LocalDate start, LocalDate end) {
        List<DietRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getRecordDate, start, end)
                        .orderByAsc(DietRecord::getRecordDate)
        );

        // key=日期，value=[kcal, protein, fat, carbo]
        Map<LocalDate, double[]> dailyMap = new LinkedHashMap<>();
        for (DietRecord r : records) {
            double[] arr = dailyMap.computeIfAbsent(r.getRecordDate(), k -> new double[4]);
            arr[0] += r.getEnergyKcal().doubleValue();
            arr[1] += r.getProteinG().doubleValue();
            arr[2] += r.getFatG().doubleValue();
            arr[3] += r.getCarboG().doubleValue();
        }

        List<WeeklyStatsVO> result = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> entry : dailyMap.entrySet()) {
            double[] v = entry.getValue();
            result.add(new WeeklyStatsVO(
                    entry.getKey().toString(),
                    Math.round(v[0] * 100) / 100.0,
                    Math.round(v[1] * 100) / 100.0,
                    Math.round(v[2] * 100) / 100.0,
                    Math.round(v[3] * 100) / 100.0
            ));
        }
        return result;
    }

    /**
     * 区间内原始饮食记录（前端自行聚合用）
     */
    public List<DietRecord> getMonthlyRecords(Long userId, LocalDate start, LocalDate end) {
        return recordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getRecordDate, start, end)
                        .orderByAsc(DietRecord::getRecordDate)
        );
    }

    /**
     * 打卡达成率：区间内有记录的天数 / 总天数
     */
    public AchievementVO getAchievement(Long userId, int range) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(range - 1L);

        List<DietRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getRecordDate, start, end)
        );

        Set<LocalDate> activeDays = records.stream()
                .map(DietRecord::getRecordDate)
                .collect(Collectors.toSet());

        double rate = range > 0 ? (double) activeDays.size() / range : 0.0;
        return new AchievementVO(range, activeDays.size(), rate);
    }
}
