package com.sky.service;

import org.springframework.stereotype.Service;

public interface ShopService {

    void setShopStatus(Integer status);

    Integer getShopStatus();
}
