package com.dailydiet.auth;

import com.dailydiet.common.BizException;
import com.dailydiet.common.enums.GoalType;
import com.dailydiet.user.entity.User;
import com.dailydiet.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WxLoginService {

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private static final String WX_LOGIN_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    public LoginResponse login(String code) {
        String url = String.format(WX_LOGIN_URL, appid, secret, code);
        String response;
        try {
            response = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("微信登录远程调用失败, code={}", code, e);
            throw new BizException("微信服务暂时不可用，请稍后重试");
        }

        try {
            JsonNode node = objectMapper.readTree(response);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                throw new RuntimeException("微信登录失败: " + node.get("errmsg").asText());
            }
            String openid = node.get("openid").asText();

            User user = userMapper.findByOpenid(openid);
            boolean isNewUser = (user == null);
            if (isNewUser) {
                user = new User();
                user.setOpenid(openid);
                user.setGoalType(GoalType.MAINTAIN.getCode());
                userMapper.insert(user);
            }

            String token = jwtUtil.generateToken(user.getId(), openid);
            return new LoginResponse(token, isNewUser);

        } catch (Exception e) {
            log.error("微信登录异常", e);
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }
}
