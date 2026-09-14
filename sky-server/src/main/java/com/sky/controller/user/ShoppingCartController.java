package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
@Api(tags = "c端购物车相关接口")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;
     @PostMapping("/add")
    public Result addShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加商品购物车信息,{}",shoppingCartDTO);
         shoppingCartService.addShoppingCart(shoppingCartDTO);
        return  Result.success();
    }
    @GetMapping("/list")
    public Result<List<ShoppingCart>> getShoppingCart(){
         List<ShoppingCart> shoppingCarts = shoppingCartService.getShoppingCart();
         return Result.success(shoppingCarts);
    }
    @DeleteMapping("/clean")
    public Result cleanShoppingCart(){
         shoppingCartService.deleteShoppingCarts();
         return Result.success();
    }
}
