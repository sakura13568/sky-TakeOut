package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品，像数据库中添加一个菜品和n条关于菜品口味描述的数据
     * @param dishDTO 关于菜品的数据
     */
    void  saveDishWithFlavor(DishDTO dishDTO);

    /**
     * 分页查询菜品服务
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageSearch(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 再删除的同时要判断被删除的菜品是否处于停售状态同时是否在套餐当中
     * @param ids
     */
    void deleteDishWithFlavorBatch(List<Long> ids);

    /**
     * 查询菜品信息和味道信息
     * @param id
     * @return
     */
    DishVO getByIdWithFlavors(Long id);

    void updateWithFlavors(DishDTO dishDTO);
}
