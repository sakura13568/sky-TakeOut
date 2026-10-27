package com.sky.service.impl;

import com.sky.constant.CacheConstants;
import com.sky.constant.StatusConstant;
import com.sky.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void setShopStatus(Integer status){
       stringRedisTemplate.opsForValue().set(CacheConstants.SHOP_STATUS,status.toString());
    }
    @Override
    public Integer getShopStatus(){
        String status = stringRedisTemplate.opsForValue().get(CacheConstants.SHOP_STATUS);
        if(status==null){
            setShopStatus(StatusConstant.DISABLE);
            return StatusConstant.DISABLE;
        }
        return Integer.valueOf(status);
    }
}
