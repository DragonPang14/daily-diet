package com.dailydiet.recipe;

import com.dailydiet.common.ResponseResult;
import com.dailydiet.recipe.dto.RecipeResponse;
import com.dailydiet.recipe.dto.SubstituteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping("/generate")
    public ResponseResult<?> generate(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recipeService.generateDaily(userId, LocalDate.now()));
    }

    @GetMapping("/today")
    public ResponseResult<?> today(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recipeService.getToday(userId));
    }

    @GetMapping("/{date}")
    public ResponseResult<?> getByDate(Authentication auth, @PathVariable String date) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recipeService.getByDate(userId, LocalDate.parse(date)));
    }

    @GetMapping("/items/{itemId}/substitute")
    public ResponseResult<?> substitutes(Authentication auth, @PathVariable Long itemId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recipeService.getSubstitutes(itemId, userId));
    }

    @PostMapping("/items/{itemId}/substitute")
    public ResponseResult<?> doSubstitute(Authentication auth,
                                           @PathVariable Long itemId,
                                           @RequestParam Long foodId) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseResult.success(recipeService.doSubstitute(itemId, foodId, userId));
    }

    @PostMapping("/items/{itemId}/dislike")
    public ResponseResult<?> dislike(Authentication auth, @PathVariable Long itemId) {
        Long userId = (Long) auth.getPrincipal();
        recipeService.markDislike(itemId, userId);
        return ResponseResult.success();
    }
}
