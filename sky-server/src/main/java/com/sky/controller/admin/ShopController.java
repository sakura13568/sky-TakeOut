package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags = "店铺相关操作")
public class ShopController {
    @Autowired
    private ShopService shopService;
    @ApiOperation("设置店铺状态")
    @PutMapping("/{status}")
    public Result<Object> setShopStatus(@PathVariable Integer status){
        log.info("设置店铺状态,{}",status);
        shopService.setShopStatus(status);
        return Result.success();
    }
    @ApiOperation("查询店铺营业状态")
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status = shopService.getShopStatus();
        return Result.success(status);
    }

}
