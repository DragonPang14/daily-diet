package com.dailydiet.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailydiet.recipe.entity.FoodDislike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FoodDislikeMapper extends BaseMapper<FoodDislike> {

    @Select("SELECT food_id, dislike_count FROM food_dislikes WHERE user_id = #{userId}")
    List<FoodDislike> findByUserId(Long userId);
}
