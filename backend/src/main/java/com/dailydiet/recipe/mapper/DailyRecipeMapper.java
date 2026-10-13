package com.dailydiet.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailydiet.recipe.entity.DailyRecipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DailyRecipeMapper extends BaseMapper<DailyRecipe> {

    @Select("SELECT * FROM daily_recipes WHERE user_id = #{userId} AND recipe_date = #{date}")
    DailyRecipe findByUserAndDate(Long userId, java.time.LocalDate date);
}
