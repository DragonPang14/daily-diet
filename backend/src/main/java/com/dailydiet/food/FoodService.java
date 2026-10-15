package com.dailydiet.food;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dailydiet.common.BizException;
import com.dailydiet.common.constant.CacheKey;
import com.dailydiet.common.constant.FoodSource;
import com.dailydiet.food.dto.CustomFoodRequest;
import com.dailydiet.food.dto.FoodSearchRequest;
import com.dailydiet.food.entity.Food;
import com.dailydiet.food.mapper.FoodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodMapper foodMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public Page<Food> search(FoodSearchRequest req) {
        Page<Food> page = new Page<>(req.getPage(), req.getSize());
        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();

        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            wrapper.like(Food::getName, req.getKeyword());
        }
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            wrapper.eq(Food::getCategory, req.getCategory());
        }
        wrapper.orderByDesc(Food::getSource);

        return foodMapper.selectPage(page, wrapper);
    }

    public Food getById(Long id) {
        Object cached = redisTemplate.opsForValue().get(CacheKey.FOOD + id);
        if (cached instanceof Food f) {
            return f;
        }

        Food food = foodMapper.selectById(id);
        if (food == null) throw new BizException("食物不存在");

        redisTemplate.opsForValue().set(CacheKey.FOOD + id, food, Duration.ofMinutes(CacheKey.FOOD_TTL_MINUTES));
        return food;
    }

    public Food createCustomFood(Long userId, CustomFoodRequest req) {
        Food f = new Food();
        f.setName(req.getName());
        f.setCategory(req.getCategory());
        f.setBrand(req.getBrand());
        f.setSource(FoodSource.CUSTOM);
        f.setEnergyKcal(req.getEnergyKcal());
        f.setProteinG(req.getProteinG());
        f.setFatG(req.getFatG());
        f.setCarboG(req.getCarboG());
        f.setCommonWeight(req.getCommonWeight());
        f.setImageUrl(req.getImageUrl());

        // 计算 PFC 向量
        double[] vec = {
            req.getCarboG().doubleValue(),
            req.getProteinG().doubleValue(),
            req.getFatG().doubleValue()
        };
        f.setPfcVector("[" + vec[0] + "," + vec[1] + "," + vec[2] + "]");

        foodMapper.insert(f);
        return f;
    }

    public Food updateCustomFood(Long id, CustomFoodRequest req) {
        Food f = foodMapper.selectById(id);
        if (f == null) throw new BizException("食物不存在");
        if (f.getSource() != FoodSource.CUSTOM) throw new BizException("只能修改自定义食物");

        f.setName(req.getName());
        f.setCategory(req.getCategory());
        f.setBrand(req.getBrand());
        f.setEnergyKcal(req.getEnergyKcal());
        f.setProteinG(req.getProteinG());
        f.setFatG(req.getFatG());
        f.setCarboG(req.getCarboG());
        f.setCommonWeight(req.getCommonWeight());
        f.setImageUrl(req.getImageUrl());

        double[] vec = {
            req.getCarboG().doubleValue(),
            req.getProteinG().doubleValue(),
            req.getFatG().doubleValue()
        };
        f.setPfcVector("[" + vec[0] + "," + vec[1] + "," + vec[2] + "]");

        foodMapper.updateById(f);
        redisTemplate.delete(CacheKey.FOOD + id);
        return f;
    }

    public void deleteCustomFood(Long id) {
        Food f = foodMapper.selectById(id);
        if (f == null) throw new BizException("食物不存在");
        if (f.getSource() != FoodSource.CUSTOM) throw new BizException("只能删除自定义食物");
        foodMapper.deleteById(id);
        redisTemplate.delete(CacheKey.FOOD + id);
    }
}
