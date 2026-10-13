package com.dailydiet.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailydiet.recipe.entity.RecipeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecipeItemMapper extends BaseMapper<RecipeItem> {

    @Select("SELECT * FROM recipe_items WHERE recipe_id = #{recipeId} ORDER BY meal_type, order_index")
    List<RecipeItem> findByRecipeId(Long recipeId);
}
