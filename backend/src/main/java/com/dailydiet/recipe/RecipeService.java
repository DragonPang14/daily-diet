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
import com.dailydiet.common.constant.FoodSource;
import com.dailydiet.common.enums.FoodCategory;
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

import lombok.RequiredArgsConstructor;

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

    /** 蛋白类/谷物类按缺口 60% 反算克数 */
    private static final double NUTRIENT_RATIO_MAIN = 0.6;

    /** 乳品类/水果类按缺口 30% 反算克数 */
    private static final double NUTRIENT_RATIO_MINOR = 0.3;

    /** 营养缺口不足或食物营养为 0 时的兜底克数 */
    private static final BigDecimal DEFAULT_WEIGHT_G = BigDecimal.valueOf(200);

    /** dislike_count 达到该值时从推荐池排除 */
    private static final int DISLIKE_EXCLUDE_THRESHOLD = 3;

    /** 替换候选返回数量 */
    private static final int SUBSTITUTE_TOP_N = 3;

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

        TargetResponse target = userService.getTarget(userId);

        DailyRecipe recipe = new DailyRecipe();
        recipe.setUserId(userId);
        recipe.setRecipeDate(date);
        recipe.setTargetKcal(target.getTargetKcal());
        recipe.setTargetCarboG(BigDecimal.valueOf(target.getTargetCarboG()));
        recipe.setTargetProteinG(BigDecimal.valueOf(target.getTargetProteinG()));
        recipe.setTargetFatG(BigDecimal.valueOf(target.getTargetFatG()));
        recipeMapper.insert(recipe);

        // 不喜欢食材只查一次，供所有餐次共用
        Set<Long> excludedFoodIds = findExcludedFoodIds(userId);

        // 每餐所需类别：蛋白 + 主食(谷物) + 蔬菜（加餐只蛋白+水果）
        Map<Integer, List<String>> mealCategories = new HashMap<>();
        mealCategories.put(MealType.BREAKFAST.getCode(),
                List.of(FoodCategory.PROTEIN.getDesc(), FoodCategory.GRAIN.getDesc(), FoodCategory.DAIRY.getDesc()));
        mealCategories.put(MealType.LUNCH.getCode(),
                List.of(FoodCategory.PROTEIN.getDesc(), FoodCategory.GRAIN.getDesc(), FoodCategory.VEGETABLE.getDesc()));
        mealCategories.put(MealType.DINNER.getCode(),
                List.of(FoodCategory.PROTEIN.getDesc(), FoodCategory.GRAIN.getDesc(), FoodCategory.VEGETABLE.getDesc()));
        mealCategories.put(MealType.SNACK.getCode(),
                List.of(FoodCategory.PROTEIN.getDesc(), FoodCategory.FRUIT.getDesc()));

        List<RecipeItem> allItems = new ArrayList<>();
        for (MealType meal : MealType.values()) {
            double ratio = MEAL_RATIOS[meal.getCode() - 1];
            List<String> categories = mealCategories.get(meal.getCode());
            if (categories == null) continue;

            allItems.addAll(fillMeal(recipe.getId(), meal.getCode(), categories,
                    target.getTargetProteinG() * ratio, target.getTargetCarboG() * ratio,
                    excludedFoodIds));
        }

        return buildResponse(recipe, allItems);
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
        return buildResponse(recipe, itemMapper.findByRecipeId(recipe.getId()));
    }

    /**
     * 替换食材：返回 Top 3 候选
     */
    public SubstituteResponse getSubstitutes(Long itemId, Long userId) {
        RecipeItem item = getOwnedItem(itemId, userId);

        Food original = foodMapper.selectById(item.getFoodId());
        if (original == null) throw new BizException("原食材不存在");

        // 查同类别、热量 ±20% 的候选
        double kcal = original.getEnergyKcal().doubleValue();
        List<Food> candidates = foodMapper.selectList(
                new LambdaQueryWrapper<Food>()
                        .eq(Food::getCategory, original.getCategory())
                        .between(Food::getEnergyKcal, kcal * 0.8, kcal * 1.2)
                        .ne(Food::getId, original.getId()));

        // 排除 dislike_count >= 阈值的
        Map<Long, Integer> dislikeMap = dislikeMapper.findByUserId(userId).stream()
                .collect(Collectors.toMap(FoodDislike::getFoodId, FoodDislike::getDislikeCount, (a, b) -> a));
        candidates = candidates.stream()
                .filter(c -> dislikeMap.getOrDefault(c.getId(), 0) < DISLIKE_EXCLUDE_THRESHOLD)
                .collect(Collectors.toList());

        // 打分排序取 Top 3
        List<Food> ranked = substitutionMatcher.rankCandidates(original, candidates, dislikeMap);
        if (ranked.size() > SUBSTITUTE_TOP_N) {
            ranked = ranked.subList(0, SUBSTITUTE_TOP_N);
        }

        return new SubstituteResponse(ranked);
    }

    /**
     * 替换食材：执行替换
     */
    @Transactional
    public RecipeItem doSubstitute(Long itemId, Long newFoodId, Long userId) {
        RecipeItem item = getOwnedItem(itemId, userId);

        Food newFood = foodMapper.selectById(newFoodId);
        if (newFood == null) throw new BizException("替换食材不存在");

        // 保持热量接近，反算克数（BigDecimal 精确计算）
        BigDecimal newFoodKcal = newFood.getEnergyKcal();
        if (newFoodKcal.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException("替换食材热量为 0，无法计算克数");
        }
        // weightG = targetKcal / newFoodKcal * 100
        BigDecimal weightG = item.getEnergyKcal()
                .divide(newFoodKcal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        item.setFoodId(newFoodId);
        item.setFoodWeightG(weightG);

        // 重算 PFC（ratio = weightG / 100）
        BigDecimal ratio = weightG.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        item.setEnergyKcal(newFood.getEnergyKcal().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        item.setProteinG(newFood.getProteinG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        item.setFatG(newFood.getFatG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        item.setCarboG(newFood.getCarboG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));

        itemMapper.updateById(item);
        return item;
    }

    /**
     * 标记不喜欢：dislike_count +1
     */
    @Transactional
    public void markDislike(Long itemId, Long userId) {
        RecipeItem item = getOwnedItem(itemId, userId);

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

    /**
     * 查询明细并校验归属当前用户，防止越权操作他人食谱
     */
    private RecipeItem getOwnedItem(Long itemId, Long userId) {
        RecipeItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BizException("食谱项不存在");

        DailyRecipe recipe = recipeMapper.selectById(item.getRecipeId());
        if (recipe == null) throw new BizException("所属食谱不存在");
        if (!recipe.getUserId().equals(userId)) throw new BizException("无权操作该食谱");

        return item;
    }

    /**
     * 统一组装食谱响应
     */
    private RecipeResponse buildResponse(DailyRecipe recipe, List<RecipeItem> items) {
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
     * 查询需从推荐池排除的食材 ID（dislike_count >= 阈值）
     */
    private Set<Long> findExcludedFoodIds(Long userId) {
        return dislikeMapper.findByUserId(userId).stream()
                .filter(d -> d.getDislikeCount() >= DISLIKE_EXCLUDE_THRESHOLD)
                .map(FoodDislike::getFoodId)
                .collect(Collectors.toSet());
    }

    /**
     * 填充一个餐次：按类别贪心选取食物并生成明细
     */
    private List<RecipeItem> fillMeal(Long recipeId, int mealType, List<String> categories,
                                      double remainingProtein, double remainingCarbo,
                                      Set<Long> excludedFoodIds) {
        List<RecipeItem> items = new ArrayList<>();
        int orderIndex = 0;

        for (String category : categories) {
            Food food = selectFoodByCategory(category, excludedFoodIds);
            if (food == null) continue;

            RecipeItem item = buildRecipeItem(recipeId, mealType, orderIndex++,
                    food, category, remainingProtein, remainingCarbo);
            itemMapper.insert(item);
            items.add(item);

            // 扣减已满足的缺口
            remainingProtein -= item.getProteinG().doubleValue();
            remainingCarbo -= item.getCarboG().doubleValue();
        }

        return items;
    }

    /**
     * 从标准库按类别随机选一个食物（排除被拉黑的食材）
     */
    private Food selectFoodByCategory(String category, Set<Long> excludedFoodIds) {
        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Food::getCategory, category)
               .eq(Food::getSource, FoodSource.STANDARD);
        if (!excludedFoodIds.isEmpty()) {
            wrapper.notIn(Food::getId, excludedFoodIds);
        }
        wrapper.last("ORDER BY RAND() LIMIT 1");
        return foodMapper.selectOne(wrapper);
    }

    /**
     * 按类别对应的营养缺口反算克数，并生成食谱明细
     */
    private RecipeItem buildRecipeItem(Long recipeId, int mealType, int orderIndex,
                                       Food food, String category,
                                       double remainingProtein, double remainingCarbo) {
        BigDecimal weightG = calcWeightG(food, category, remainingProtein, remainingCarbo);
        BigDecimal ratio = weightG.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        RecipeItem item = new RecipeItem();
        item.setRecipeId(recipeId);
        item.setMealType(mealType);
        item.setFoodId(food.getId());
        item.setFoodWeightG(weightG);
        item.setEnergyKcal(food.getEnergyKcal().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        item.setProteinG(food.getProteinG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        item.setFatG(food.getFatG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        item.setCarboG(food.getCarboG().multiply(ratio).setScale(2, RoundingMode.HALF_UP));
        item.setOrderIndex(orderIndex);
        return item;
    }

    /**
     * 按类别反算推荐克数：蛋白/谷物贡献缺口 60%，乳品/水果 30%，其余兜底 200g
     */
    private BigDecimal calcWeightG(Food food, String category,
                                   double remainingProtein, double remainingCarbo) {
        double targetNutrient;
        double nutrientPer100g;

        if (FoodCategory.PROTEIN.getDesc().equals(category)) {
            targetNutrient = remainingProtein * NUTRIENT_RATIO_MAIN;
            nutrientPer100g = food.getProteinG().doubleValue();
        } else if (FoodCategory.GRAIN.getDesc().equals(category)) {
            targetNutrient = remainingCarbo * NUTRIENT_RATIO_MAIN;
            nutrientPer100g = food.getCarboG().doubleValue();
        } else if (FoodCategory.DAIRY.getDesc().equals(category)) {
            targetNutrient = remainingProtein * NUTRIENT_RATIO_MINOR;
            nutrientPer100g = food.getProteinG().doubleValue();
        } else if (FoodCategory.FRUIT.getDesc().equals(category)) {
            targetNutrient = remainingCarbo * NUTRIENT_RATIO_MINOR;
            nutrientPer100g = food.getCarboG().doubleValue();
        } else {
            // 蔬菜等低热量类别不参与反算
            return DEFAULT_WEIGHT_G;
        }

        if (nutrientPer100g <= 0 || targetNutrient <= 0) {
            return DEFAULT_WEIGHT_G;
        }
        return BigDecimal.valueOf(targetNutrient / nutrientPer100g * 100)
                .setScale(0, RoundingMode.HALF_UP);
    }
}
