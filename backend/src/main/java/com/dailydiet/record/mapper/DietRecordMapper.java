package com.dailydiet.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailydiet.record.entity.DietRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DietRecordMapper extends BaseMapper<DietRecord> {

    @Select("SELECT * FROM diet_records WHERE user_id = #{userId} AND record_date = #{date} ORDER BY meal_type, created_at")
    List<DietRecord> findByUserAndDate(Long userId, LocalDate date);
}
