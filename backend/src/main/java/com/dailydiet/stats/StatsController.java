package com.dailydiet.stats;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dailydiet.common.ResponseResult;
import com.dailydiet.record.entity.DietRecord;
import com.dailydiet.record.mapper.DietRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final DietRecordMapper recordMapper;

    @GetMapping("/weekly")
    public ResponseResult<?> weekly(Authentication auth, @RequestParam(required = false) String startDate) {
        Long userId = (Long) auth.getPrincipal();
        LocalDate end = LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(6);

        List<DietRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getRecordDate, start, end)
                        .orderByAsc(DietRecord::getRecordDate)
        );

        Map<LocalDate, double[]> dailyMap = new LinkedHashMap<>();
        for (DietRecord r : records) {
            double[] arr = dailyMap.computeIfAbsent(r.getRecordDate(),
                    k -> new double[4]);
            arr[0] += r.getEnergyKcal().doubleValue();
            arr[1] += r.getProteinG().doubleValue();
            arr[2] += r.getFatG().doubleValue();
            arr[3] += r.getCarboG().doubleValue();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> entry : dailyMap.entrySet()) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", entry.getKey().toString());
            day.put("kcal", Math.round(entry.getValue()[0] * 100) / 100.0);
            day.put("proteinG", Math.round(entry.getValue()[1] * 100) / 100.0);
            day.put("fatG", Math.round(entry.getValue()[2] * 100) / 100.0);
            day.put("carboG", Math.round(entry.getValue()[3] * 100) / 100.0);
            result.add(day);
        }

        return ResponseResult.success(result);
    }

    @GetMapping("/monthly")
    public ResponseResult<?> monthly(Authentication auth, @RequestParam(required = false) String startDate) {
        Long userId = (Long) auth.getPrincipal();
        LocalDate end = LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(29);
        return ResponseResult.success(recordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getRecordDate, start, end)
                        .orderByAsc(DietRecord::getRecordDate)
        ));
    }

    @GetMapping("/achievement")
    public ResponseResult<?> achievement(Authentication auth, @RequestParam(defaultValue = "7") int range) {
        Long userId = (Long) auth.getPrincipal();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(range - 1);

        List<DietRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getRecordDate, start, end)
        );

        Set<LocalDate> activeDays = new HashSet<>();
        for (DietRecord r : records) {
            activeDays.add(r.getRecordDate());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalDays", range);
        result.put("activeDays", activeDays.size());
        result.put("achievementRate", (double) activeDays.size() / range);
        return ResponseResult.success(result);
    }
}
