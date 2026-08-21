package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 在添加菜品的同时向数据库中添加菜品口味
     * @param  flavors 多组口味描述的数据
     */
    void  addDishFlavorBatch(@Param("flavors") List<DishFlavor> flavors);
    /**
     * 批量删除菜品口味
     * @param ids 菜品id集合
     */
    void deleteByDishIds(@Param("ids") List<Long> ids);
    @Select("select * from dish_flavor where dish_id = #{id}")
    List<DishFlavor> getFlavorsById(Long id);
}
