package com.dailydiet.stats;

import com.dailydiet.common.ResponseResult;
import com.dailydiet.record.entity.DietRecord;
import com.dailydiet.stats.service.StatsService;
import com.dailydiet.stats.vo.AchievementVO;
import com.dailydiet.stats.vo.WeeklyStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/weekly")
    public ResponseResult<List<WeeklyStatsVO>> weekly(
            Authentication auth, @RequestParam(required = false) String startDate) {
        Long userId = (Long) auth.getPrincipal();
        LocalDate end = LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(6);
        return ResponseResult.success(statsService.getDailyStats(userId, start, end));
    }

    @GetMapping("/monthly")
    public ResponseResult<List<DietRecord>> monthly(
            Authentication auth, @RequestParam(required = false) String startDate) {
        Long userId = (Long) auth.getPrincipal();
        LocalDate end = LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(29);
        return ResponseResult.success(statsService.getMonthlyRecords(userId, start, end));
    }

    @GetMapping("/achievement")
    public ResponseResult<AchievementVO> achievement(
            Authentication auth, @RequestParam(defaultValue = "7") int range) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(statsService.getAchievement(userId, range));
    }
}
