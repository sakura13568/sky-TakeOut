package com.sky.service.impl;

import com.sky.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {
    private static final String key = "SHOP_STATUS";
    @Autowired
    private RedisTemplate<Object,Object> redisTemplate;
    @Override
    //TODO尝试将stringOperations提取出来
    public void setShopStatus(Integer status){
        ValueOperations<Object, Object> stringOperations = redisTemplate.opsForValue();
        stringOperations.set(key,String.valueOf(status));
    }
    @Override
    public Integer getShopStatus(){
        ValueOperations<Object,Object> stingOperations = redisTemplate.opsForValue();
        String shopStatus = (String)stingOperations.get(key);
        //todo弄懂assert作用
        assert shopStatus != null;
        Integer status = Integer.parseInt(shopStatus);
        return status;
    }
}
