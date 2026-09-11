package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;

    private static final String WXLOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Override
    public User wechatLogIn(UserLoginDTO userLoginDTO){
        String openId = getOpenId(userLoginDTO.getCode());
        if(openId == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        User user = userMapper.getUserByOpenId(openId);
        if(user == null){
            user = User.builder()
                            .openid(openId)
                                    .createTime(LocalDateTime.now())
                                            .build();
            userMapper.insert(user);
        }
        return user;
    }

    /**
     *调用微信接口获得用户唯一标识openId
     * @param code 临时登录凭证
     * @return
     */
    private String getOpenId(String code){
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("grant_type", "authorization_code");
        paramMap.put("js_code", code);
        paramMap.put("appid",weChatProperties.getAppid());
        paramMap.put("secret",weChatProperties.getSecret());
        return HttpClientUtil.doGet(WXLOGIN_URL, paramMap);
    }
}
