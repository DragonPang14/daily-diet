package com.dailydiet.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dailydiet.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE openid = #{openid}")
    User findByOpenid(String openid);
}
