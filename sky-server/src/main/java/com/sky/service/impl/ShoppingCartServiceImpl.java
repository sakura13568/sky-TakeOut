package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO){
       ShoppingCart shoppingCart = new ShoppingCart();
       BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectShoppingCartList(shoppingCart);
        if(shoppingCarts != null && !shoppingCarts.isEmpty()){
            ShoppingCart userShoppingCart = shoppingCarts.get(0);
            userShoppingCart.setNumber(userShoppingCart.getNumber()+1);
            shoppingCartMapper.updateNumberById(userShoppingCart);
        }
        else{
            Long dishId = shoppingCart.getDishId();
            Long setmealId = shoppingCart.getSetmealId();
            if(dishId != null){
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }else{
                if(setmealId == null){
                    throw new RuntimeException("购物车商品缺少套餐id");
                }
                Setmeal setmeal = setmealMapper.getById(setmealId);
                if(setmeal == null){
                    throw new RuntimeException("套餐不存在");
                }
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.addShoppingCart(shoppingCart);
        }
    }
    @Override
    public List<ShoppingCart> getShoppingCart(){
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.getShoppingCartByUserId(userId);
        return shoppingCarts;
    }
    @Override
    public void deleteShoppingCarts(){
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteShopppingCartsByUserId(userId);
    }
}
