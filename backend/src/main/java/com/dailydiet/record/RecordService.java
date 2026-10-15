package com.dailydiet.record;

import com.dailydiet.common.BizException;
import com.dailydiet.food.entity.Food;
import com.dailydiet.food.mapper.FoodMapper;
import com.dailydiet.recipe.entity.RecipeItem;
import com.dailydiet.recipe.mapper.RecipeItemMapper;
import com.dailydiet.record.dto.*;
import com.dailydiet.record.entity.DietRecord;
import com.dailydiet.record.mapper.DietRecordMapper;
import com.dailydiet.user.UserService;
import com.dailydiet.user.dto.TargetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final DietRecordMapper recordMapper;
    private final FoodMapper foodMapper;
    private final RecipeItemMapper recipeItemMapper;
    private final UserService userService;

    @Transactional
    public DietRecord add(Long userId, AddRecordRequest req) {
        Food food = foodMapper.selectById(req.getFoodId());
        if (food == null) throw new BizException("食物不存在");

        double weightG = req.getFoodWeightG().doubleValue();
        double ratio = weightG / 100;

        DietRecord r = new DietRecord();
        r.setUserId(userId);
        r.setRecordDate(LocalDate.now());
        r.setMealType(req.getMealType());
        r.setFoodId(req.getFoodId());
        r.setFoodWeightG(req.getFoodWeightG());
        r.setEnergyKcal(BigDecimal.valueOf(food.getEnergyKcal().doubleValue() * ratio)
                .setScale(2, RoundingMode.HALF_UP));
        r.setProteinG(BigDecimal.valueOf(food.getProteinG().doubleValue() * ratio)
                .setScale(2, RoundingMode.HALF_UP));
        r.setFatG(BigDecimal.valueOf(food.getFatG().doubleValue() * ratio)
                .setScale(2, RoundingMode.HALF_UP));
        r.setCarboG(BigDecimal.valueOf(food.getCarboG().doubleValue() * ratio)
                .setScale(2, RoundingMode.HALF_UP));
        r.setSource(2); // 手动添加
        r.setRecipeItemId(req.getRecipeItemId());

        recordMapper.insert(r);
        return r;
    }

    @Transactional
    public void bulkFromRecipe(Long userId, Long recipeId) {
        List<RecipeItem> items = recipeItemMapper.findByRecipeId(recipeId);
        for (RecipeItem item : items) {
            DietRecord r = new DietRecord();
            r.setUserId(userId);
            r.setRecordDate(LocalDate.now());
            r.setMealType(item.getMealType());
            r.setFoodId(item.getFoodId());
            r.setFoodWeightG(item.getFoodWeightG());
            r.setEnergyKcal(item.getEnergyKcal());
            r.setProteinG(item.getProteinG());
            r.setFatG(item.getFatG());
            r.setCarboG(item.getCarboG());
            r.setSource(1); // 一键入库
            r.setRecipeItemId(item.getId());
            recordMapper.insert(r);
        }
    }

    @Transactional
    public DietRecord update(Long id, Long userId, AddRecordRequest req) {
        DietRecord r = recordMapper.selectById(id);
        if (r == null || !r.getUserId().equals(userId)) throw new BizException("记录不存在");

        Food food = foodMapper.selectById(req.getFoodId());
        if (food == null) throw new BizException("食物不存在");

        double weightG = req.getFoodWeightG().doubleValue();
        double ratio = weightG / 100;

        r.setMealType(req.getMealType());
        r.setFoodId(req.getFoodId());
        r.setFoodWeightG(req.getFoodWeightG());
        r.setEnergyKcal(BigDecimal.valueOf(food.getEnergyKcal().doubleValue() * ratio)
                .setScale(2, RoundingMode.HALF_UP));
        r.setProteinG(BigDecimal.valueOf(food.getProteinG().doubleValue() * ratio)
                .setScale(2, RoundingMode.HALF_UP));
        r.setFatG(BigDecimal.valueOf(food.getFatG().doubleValue() * ratio)
                .setScale(2, RoundingMode.HALF_UP));
        r.setCarboG(BigDecimal.valueOf(food.getCarboG().doubleValue() * ratio)
                .setScale(2, RoundingMode.HALF_UP));

        recordMapper.updateById(r);
        return r;
    }

    public void delete(Long id, Long userId) {
        DietRecord r = recordMapper.selectById(id);
        if (r == null || !r.getUserId().equals(userId)) throw new BizException("记录不存在");
        recordMapper.deleteById(id);
    }

    public DailySummary getTodaySummary(Long userId) {
        return getSummaryByDate(userId, LocalDate.now());
    }

    public DailySummary getSummaryByDate(Long userId, LocalDate date) {
        List<DietRecord> records = recordMapper.findByUserAndDate(userId, date);

        BigDecimal totalKcal = BigDecimal.ZERO;
        BigDecimal totalProtein = BigDecimal.ZERO;
        BigDecimal totalFat = BigDecimal.ZERO;
        BigDecimal totalCarbo = BigDecimal.ZERO;
        for (DietRecord r : records) {
            totalKcal = totalKcal.add(r.getEnergyKcal());
            totalProtein = totalProtein.add(r.getProteinG());
            totalFat = totalFat.add(r.getFatG());
            totalCarbo = totalCarbo.add(r.getCarboG());
        }

        TargetResponse target = userService.getTarget(userId);
        BigDecimal targetKcal = BigDecimal.valueOf(target.getTargetKcal());

        DailySummary s = new DailySummary();
        s.setTotalKcal(totalKcal.setScale(2, RoundingMode.HALF_UP));
        s.setTotalProteinG(totalProtein.setScale(2, RoundingMode.HALF_UP));
        s.setTotalFatG(totalFat.setScale(2, RoundingMode.HALF_UP));
        s.setTotalCarboG(totalCarbo.setScale(2, RoundingMode.HALF_UP));
        s.setTargetKcal(target.getTargetKcal());
        s.setTargetProteinG(BigDecimal.valueOf(target.getTargetProteinG()));
        s.setTargetFatG(BigDecimal.valueOf(target.getTargetFatG()));
        s.setTargetCarboG(BigDecimal.valueOf(target.getTargetCarboG()));
        // 达成率，分母为 0 时安全返回 0
        double rate = targetKcal.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalKcal.divide(targetKcal, 4, RoundingMode.HALF_UP).doubleValue();
        s.setAchievementRate(rate);
        s.setRecords(records);
        return s;
    }

    public List<DietRecord> getByDate(Long userId, LocalDate date) {
        return recordMapper.findByUserAndDate(userId, date);
    }
}
