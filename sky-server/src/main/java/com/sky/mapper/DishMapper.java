package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 向数据库中添加新的菜品
     * @param dish
     */
    @AutoFill(OperationType.INSERT)
    void  addDish(Dish dish);

    /**
     * 分页查询数据库中与之相关的菜品信息
     * @param dishPageQueryDTO
     * @return
     */
    Page<Dish> pageSearch(DishPageQueryDTO dishPageQueryDTO);
     @Select("select d.status from dish d where id = #{id}")
     Integer getDishStatusById(Long id);
    /**
     * 批量删除菜品
     * @param ids 菜品id集合
     */
    void deleteDishBatch(@Param("ids") List<Long> ids);
    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);
    @Select("select * from dish where category_id = #{categoryId} and status = #{status}")
    List<Dish> list(@Param ("categoryId") Long categoryId,@Param("status") Integer status);

}
