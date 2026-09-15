package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 通过用户id(user_id)获得该用户的所有购物车信息
     * @return
     */
    List<ShoppingCart> getShoppingCart();

    /**
     * 通过user_id删除购物车中的信息
     */
    void deleteAllShoppingCarts();

    void subShoppingCarts(ShoppingCartDTO shoppingCartDTO);
}
