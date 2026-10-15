package com.dailydiet.recipe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dailydiet.algorithm.SubstitutionMatcher;
import com.dailydiet.common.BizException;
import com.dailydiet.common.enums.MealType;
import com.dailydiet.food.entity.Food;
import com.dailydiet.food.mapper.FoodMapper;
import com.dailydiet.recipe.dto.RecipeResponse;
import com.dailydiet.recipe.dto.SubstituteResponse;
import com.dailydiet.recipe.entity.DailyRecipe;
import com.dailydiet.recipe.entity.FoodDislike;
import com.dailydiet.recipe.entity.RecipeItem;
import com.dailydiet.recipe.mapper.DailyRecipeMapper;
import com.dailydiet.recipe.mapper.FoodDislikeMapper;
import com.dailydiet.recipe.mapper.RecipeItemMapper;
import com.dailydiet.user.UserService;
import com.dailydiet.user.dto.TargetResponse;
import com.dailydiet.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final DailyRecipeMapper recipeMapper;
    private final RecipeItemMapper itemMapper;
    private final FoodMapper foodMapper;
    private final FoodDislikeMapper dislikeMapper;
    private final SubstitutionMatcher substitutionMatcher;
    private final UserService userService;

    // 餐次比例：早 25% · 午 35% · 晚 30% · 加餐 10%
    private static final double[] MEAL_RATIOS = {0.25, 0.35, 0.30, 0.10};

    /**
     * 生成当日食谱
     */
    @Transactional
    public RecipeResponse generateDaily(Long userId, LocalDate date) {
        // 查已有则先删重建
        DailyRecipe existing = recipeMapper.findByUserAndDate(userId, date);
        if (existing != null) {
            itemMapper.delete(new LambdaQueryWrapper<RecipeItem>()
                    .eq(RecipeItem::getRecipeId, existing.getId()));
            recipeMapper.deleteById(existing.getId());
        }

        User user = userService.getById(userId);
        TargetResponse target = userService.getTarget(userId);

        DailyRecipe recipe = new DailyRecipe();
        recipe.setUserId(userId);
        recipe.setRecipeDate(date);
        recipe.setTargetKcal(target.getTargetKcal());
        recipe.setTargetCarboG(BigDecimal.valueOf(target.getTargetCarboG()));
        recipe.setTargetProteinG(BigDecimal.valueOf(target.getTargetProteinG()));
        recipe.setTargetFatG(BigDecimal.valueOf(target.getTargetFatG()));

        recipeMapper.insert(recipe);

        // 按餐次生成
        List<RecipeItem> allItems = new ArrayList<>();
        // 每餐所需类别：蛋白 + 主食(谷物) + 蔬菜（加餐只蛋白+水果）
        Map<Integer, List<String>> mealCategories = new HashMap<>();
        mealCategories.put(MealType.BREAKFAST.getCode(), List.of("蛋白", "谷物", "乳品"));
        mealCategories.put(MealType.LUNCH.getCode(), List.of("蛋白", "谷物", "蔬菜"));
        mealCategories.put(MealType.DINNER.getCode(), List.of("蛋白", "谷物", "蔬菜"));
        mealCategories.put(MealType.SNACK.getCode(), List.of("蛋白", "水果"));

        for (MealType meal : MealType.values()) {
            double mealKcal = target.getTargetKcal() * MEAL_RATIOS[meal.getCode() - 1];
            double mealCarbo = target.getTargetCarboG() * MEAL_RATIOS[meal.getCode() - 1];
            double mealProtein = target.getTargetProteinG() * MEAL_RATIOS[meal.getCode() - 1];
            double mealFat = target.getTargetFatG() * MEAL_RATIOS[meal.getCode() - 1];

            List<String> categories = mealCategories.get(meal.getCode());
            if (categories == null) continue;

            List<RecipeItem> mealItems = fillMeal(recipe.getId(), meal.getCode(),
                    categories, mealKcal, mealCarbo, mealProtein, mealFat, userId);
            allItems.addAll(mealItems);
        }

        RecipeResponse resp = new RecipeResponse();
        resp.setId(recipe.getId());
        resp.setRecipeDate(recipe.getRecipeDate());
        resp.setTargetKcal(recipe.getTargetKcal());
        resp.setTargetCarboG(recipe.getTargetCarboG());
        resp.setTargetProteinG(recipe.getTargetProteinG());
        resp.setTargetFatG(recipe.getTargetFatG());
        resp.setItems(allItems);
        return resp;
    }

    /**
     * 获取今日食谱（若无则自动生成）
     */
    public RecipeResponse getToday(Long userId) {
        return getByDate(userId, LocalDate.now());
    }

    public RecipeResponse getByDate(Long userId, LocalDate date) {
        DailyRecipe recipe = recipeMapper.findByUserAndDate(userId, date);
        if (recipe == null) {
            return generateDaily(userId, date);
        }
        List<RecipeItem> items = itemMapper.findByRecipeId(recipe.getId());

        RecipeResponse resp = new RecipeResponse();
        resp.setId(recipe.getId());
        resp.setRecipeDate(recipe.getRecipeDate());
        resp.setTargetKcal(recipe.getTargetKcal());
        resp.setTargetCarboG(recipe.getTargetCarboG());
        resp.setTargetProteinG(recipe.getTargetProteinG());
        resp.setTargetFatG(recipe.getTargetFatG());
        resp.setItems(items);
        return resp;
    }

    /**
     * 替换食材：返回 Top 3 候选
     */
    public SubstituteResponse getSubstitutes(Long itemId, Long userId) {
        RecipeItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BizException("食谱项不存在");

        Food original = foodMapper.selectById(item.getFoodId());
        if (original == null) throw new BizException("原食材不存在");

        // 查同类别、热量 ±20% 的候选
        double minKcal = original.getEnergyKcal().doubleValue() * 0.8;
        double maxKcal = original.getEnergyKcal().doubleValue() * 1.2;

        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Food::getCategory, original.getCategory())
               .between(Food::getEnergyKcal, minKcal, maxKcal)
               .ne(Food::getId, original.getId());

        List<Food> candidates = foodMapper.selectList(wrapper);

        // 获取用户 dislike 记录
        List<FoodDislike> dislikes = dislikeMapper.findByUserId(userId);
        Map<Long, Integer> dislikeMap = dislikes.stream()
                .collect(Collectors.toMap(FoodDislike::getFoodId, FoodDislike::getDislikeCount));

        // 排除 dislike_count >= 3 的
        candidates = candidates.stream()
                .filter(c -> dislikeMap.getOrDefault(c.getId(), 0) < 3)
                .collect(Collectors.toList());

        // 打分排序取 Top 3
        List<Food> ranked = substitutionMatcher.rankCandidates(original, candidates, dislikeMap);
        if (ranked.size() > 3) ranked = ranked.subList(0, 3);

        return new SubstituteResponse(ranked);
    }

    /**
     * 替换食材：执行替换
     */
    @Transactional
    public RecipeItem doSubstitute(Long itemId, Long newFoodId, Long userId) {
        RecipeItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BizException("食谱项不存在");

        Food newFood = foodMapper.selectById(newFoodId);
        if (newFood == null) throw new BizException("替换食材不存在");

        // 保持热量接近，反算克数
        double targetKcal = item.getEnergyKcal().doubleValue();
        double weightG = targetKcal / newFood.getEnergyKcal().doubleValue() * 100;

        item.setFoodId(newFoodId);
        item.setFoodWeightG(BigDecimal.valueOf(Math.round(weightG * 100) / 100.0));

        // 重算 PFC
        double ratio = weightG / 100;
        item.setEnergyKcal(BigDecimal.valueOf(newFood.getEnergyKcal().doubleValue() * ratio));
        item.setProteinG(BigDecimal.valueOf(newFood.getProteinG().doubleValue() * ratio));
        item.setFatG(BigDecimal.valueOf(newFood.getFatG().doubleValue() * ratio));
        item.setCarboG(BigDecimal.valueOf(newFood.getCarboG().doubleValue() * ratio));

        itemMapper.updateById(item);
        return item;
    }

    /**
     * 标记不喜欢：dislike_count +1
     */
    @Transactional
    public void markDislike(Long itemId, Long userId) {
        RecipeItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BizException("食谱项不存在");

        FoodDislike existing = dislikeMapper.selectOne(
                new LambdaQueryWrapper<FoodDislike>()
                        .eq(FoodDislike::getUserId, userId)
                        .eq(FoodDislike::getFoodId, item.getFoodId())
        );

        if (existing != null) {
            existing.setDislikeCount(existing.getDislikeCount() + 1);
            dislikeMapper.updateById(existing);
        } else {
            FoodDislike fd = new FoodDislike();
            fd.setUserId(userId);
            fd.setFoodId(item.getFoodId());
            fd.setDislikeCount(1);
            dislikeMapper.insert(fd);
        }
    }

    // === 私有方法 ===

    private List<RecipeItem> fillMeal(Long recipeId, int mealType,
                                       List<String> categories,
                                       double mealKcal, double mealCarbo,
                                       double mealProtein, double mealFat,
                                       Long userId) {

        List<RecipeItem> items = new ArrayList<>();
        int orderIndex = 0;

        // 简化贪心：每类选一个食物，按目标 PFC 反算克数
        double remainingKcal = mealKcal;
        double remainingProtein = mealProtein;
        double remainingCarbo = mealCarbo;
        double remainingFat = mealFat;

        List<FoodDislike> dislikes = dislikeMapper.findByUserId(userId);
        Set<Long> dislikeFoods = dislikes.stream()
                .filter(d -> d.getDislikeCount() >= 3)
                .map(FoodDislike::getFoodId)
                .collect(Collectors.toSet());

        for (String category : categories) {
            // 该类别中随机选一个（排除 disliked）
            LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Food::getCategory, category)
                   .eq(Food::getSource, 1); // 标准库
            if (!dislikeFoods.isEmpty()) {
                wrapper.notIn(Food::getId, dislikeFoods);
            }
            wrapper.last("ORDER BY RAND() LIMIT 1");
            Food food = foodMapper.selectOne(wrapper);
            if (food == null) continue;

            // 该食物贡献主要营养：按蛋白或碳水或脂肪的缺口反算克数
            double targetNutrient;
            double foodNutrientPer100g;

            if (category.equals("蛋白")) {
                targetNutrient = remainingProtein * 0.6;
                foodNutrientPer100g = food.getProteinG().doubleValue();
            } else if (category.equals("谷物")) {
                targetNutrient = remainingCarbo * 0.6;
                foodNutrientPer100g = food.getCarboG().doubleValue();
            } else if (category.equals("乳品")) {
                targetNutrient = remainingProtein * 0.3;
                foodNutrientPer100g = food.getProteinG().doubleValue();
            } else if (category.equals("水果")) {
                targetNutrient = remainingCarbo * 0.3;
                foodNutrientPer100g = food.getCarboG().doubleValue();
            } else {
                // 蔬菜：热量低，固定 200g
                targetNutrient = 0;
                foodNutrientPer100g = 0;
            }

            double weightG;
            if (foodNutrientPer100g > 0 && targetNutrient > 0) {
                weightG = targetNutrient / foodNutrientPer100g * 100;
            } else {
                weightG = 200; // 蔬菜默认 200g
            }
            weightG = Math.round(weightG);

            double ratio = weightG / 100;
            RecipeItem item = new RecipeItem();
            item.setRecipeId(recipeId);
            item.setMealType(mealType);
            item.setFoodId(food.getId());
            item.setFoodWeightG(BigDecimal.valueOf(weightG));
            item.setEnergyKcal(BigDecimal.valueOf(food.getEnergyKcal().doubleValue() * ratio)
                    .setScale(2, RoundingMode.HALF_UP));
            item.setProteinG(BigDecimal.valueOf(food.getProteinG().doubleValue() * ratio)
                    .setScale(2, RoundingMode.HALF_UP));
            item.setFatG(BigDecimal.valueOf(food.getFatG().doubleValue() * ratio)
                    .setScale(2, RoundingMode.HALF_UP));
            item.setCarboG(BigDecimal.valueOf(food.getCarboG().doubleValue() * ratio)
                    .setScale(2, RoundingMode.HALF_UP));
            item.setOrderIndex(orderIndex++);

            itemMapper.insert(item);
            items.add(item);

            // 扣减剩余
            remainingKcal -= item.getEnergyKcal().doubleValue();
            remainingProtein -= item.getProteinG().doubleValue();
            remainingCarbo -= item.getCarboG().doubleValue();
            remainingFat -= item.getFatG().doubleValue();
        }

        return items;
    }
}
