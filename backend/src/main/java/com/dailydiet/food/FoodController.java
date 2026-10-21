package com.dailydiet.food;

import com.dailydiet.common.ResponseResult;
import com.dailydiet.food.dto.CustomFoodRequest;
import com.dailydiet.food.dto.FoodSearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public ResponseResult<?> search(FoodSearchRequest req) {
        return ResponseResult.success(foodService.search(req));
    }

    @GetMapping("/{id}")
    public ResponseResult<?> get(@PathVariable Long id) {
        return ResponseResult.success(foodService.getById(id));
    }

    @PostMapping("/custom")
    public ResponseResult<?> createCustom(Authentication auth, @Valid @RequestBody CustomFoodRequest req) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(foodService.createCustomFood(userId, req));
    }

    @PutMapping("/custom/{id}")
    public ResponseResult<?> updateCustom(@PathVariable Long id, @Valid @RequestBody CustomFoodRequest req) {
        return ResponseResult.success(foodService.updateCustomFood(id, req));
    }

    @DeleteMapping("/custom/{id}")
    public ResponseResult<?> deleteCustom(@PathVariable Long id) {
        foodService.deleteCustomFood(id);
        return ResponseResult.success();
    }
}
