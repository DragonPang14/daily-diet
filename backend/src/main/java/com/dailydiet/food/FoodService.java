package com.dailydiet.food;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dailydiet.common.BizException;
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
        Food f = (Food) redisTemplate.opsForValue().get("food:" + id);
        if (f != null) return f;

        f = foodMapper.selectById(id);
        if (f == null) throw new BizException("食物不存在");

        redisTemplate.opsForValue().set("food:" + id, f, Duration.ofMinutes(30));
        return f;
    }

    public Food createCustomFood(Long userId, CustomFoodRequest req) {
        Food f = new Food();
        f.setName(req.getName());
        f.setCategory(req.getCategory());
        f.setBrand(req.getBrand());
        f.setSource(2); // 用户自定义
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
        if (f.getSource() != 2) throw new BizException("只能修改自定义食物");

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
        redisTemplate.delete("food:" + id);
        return f;
    }

    public void deleteCustomFood(Long id) {
        Food f = foodMapper.selectById(id);
        if (f == null) throw new BizException("食物不存在");
        if (f.getSource() != 2) throw new BizException("只能删除自定义食物");
        foodMapper.deleteById(id);
        redisTemplate.delete("food:" + id);
    }
}
