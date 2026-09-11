package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
@Api(tags = "店铺相关操作")
public class ShopController {
    @Autowired
    private ShopService shopService;
    @ApiOperation("查询店铺营业状态")
    @GetMapping("/status")
    //todo user端数据课admin端关于店铺状态的转换不能实时同步
    public Result<Integer> getStatus(){
        Integer status = shopService.getShopStatus();
        return Result.success(status);
    }

}
