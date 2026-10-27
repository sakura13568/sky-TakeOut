package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.CacheConstants;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.utils.CacheClient;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private CacheClient cacheClient;

    @Override
    @Transactional
    public void saveDishWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.addDish(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        Long id = dish.getId();
        for (DishFlavor dishFlavor : flavors) {
            dishFlavor.setDishId(id);
        }
        dishFlavorMapper.addDishFlavorBatch(flavors);

        cleanDishCategoryCache(Collections.singletonList(dish.getCategoryId())) ;
        cleanSetmealDishCache(Collections.singletonList(id));
    }

    @Override
    public PageResult pageSearch(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<Dish> page = dishMapper.pageSearch(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }
    @Override
    public List<Dish> selectDishItemsByCategroyId(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.selectDishItemsByCategroyId(dish);
    }
    @Override
    @Transactional
    public void deleteDishWithFlavorBatch(List<Long> ids) {
        List<Long> categoryIds = new ArrayList<>();
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish != null && StatusConstant.ENABLE.equals(dish.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            if (dish != null) {
                categoryIds.add(dish.getCategoryId());
            }
        }
        List<Long> setmeals = setmealDishMapper.selectSetmealByDishId(ids);
        if (setmeals != null && !setmeals.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        dishMapper.deleteDishBatch(ids);
        dishFlavorMapper.deleteByDishIds(ids);
        cleanDishCategoryCache(categoryIds) ;
        cleanSetmealDishCache(ids);
    }

    @Override
    public DishVO getByIdWithFlavors(Long id) {
        Dish dish = dishMapper.getById(id);
        List<DishFlavor> flavors = dishFlavorMapper.getFlavorsById(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        return dishVO;
    }

    @Override
    @Transactional
    public void updateWithFlavors(DishDTO dishDTO) {
        Dish oldDish = dishMapper.getById(dishDTO.getId());
        Long oldCategoryId = oldDish == null ? null : oldDish.getCategoryId();

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        List<Long> ids = new ArrayList<>();
        ids.add(dishDTO.getId());
        dishFlavorMapper.deleteByDishIds(ids);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        for (DishFlavor dishFlavor : flavors) {
            dishFlavor.setDishId(dishDTO.getId());
        }
        dishFlavorMapper.addDishFlavorBatch(flavors);

        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(oldCategoryId);
        categoryIds.add(dishDTO.getCategoryId());
        cleanDishCategoryCache(categoryIds);
        cleanSetmealDishCache(ids);
    }

    @Override
    @Transactional
    public void setStatus(Long id, Integer status) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        Dish currentDish = dishMapper.getById(id);
        if (currentDish != null) {
             cleanDishCategoryCache(Collections.singletonList(currentDish.getCategoryId()));
        }

        if (Objects.equals(status, StatusConstant.DISABLE)) {
            // 如果是停售操作，还需要将包含当前菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            List<Long> setmealIds = setmealDishMapper.selectSetmealByDishId(dishIds);
            if (setmealIds != null && !setmealIds.isEmpty()) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
            cleanSetmealListCache(setmealIds);
        }
    }

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        String key = CacheConstants.DISH_LIST + dish.getCategoryId() + ":" +  dish.getStatus();
        return cacheClient.queryListWithMutex(key,
                DishVO.class,
                () -> loadDishFromDB(dish),
                CacheConstants.TTL_SHORT);

    }
    private List<DishVO> loadDishFromDB(Dish dish) {
        List<DishVO> dishVOList = new ArrayList<>();

        List<Dish> dishList = dishMapper.list(dish.getCategoryId(), dish.getStatus());

        dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getFlavorsById(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        return  dishVOList;
    }

    private void cleanDishCategoryCache(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(categoryId -> {
                    cacheClient.delCache(CacheConstants.DISH_LIST + categoryId + ":1");
                    cacheClient.delCache(CacheConstants.DISH_LIST + categoryId + ":0");
                });
    }
    public void cleanSetmealDishCache(List<Long> dishIds){
        if (dishIds == null || dishIds.isEmpty()) {
            return;
        }
        List<Long> setMealIds = setmealDishMapper.selectSetmealByDishId(dishIds);
        if (setMealIds == null || setMealIds.isEmpty()) {
            return;
        }
        setMealIds.forEach(setMealId -> {
            cacheClient.delCache(CacheConstants.SETMEAL_DISH + setMealId);
        });
    }
    private void cleanSetmealListCache(List<Long> setmealIds) {
        if (setmealIds == null || setmealIds.isEmpty()) {
            return;
        }
        // 批量查出这些套餐的 categoryId（新增一个 mapper 方法，避免 N 次 getById）
        List<Long> categoryIds = setmealMapper.listCategoryIdsByIds(setmealIds);
        categoryIds.stream().filter(Objects::nonNull).distinct()
                .forEach(categoryId -> {
                    cacheClient.delCache(CacheConstants.SETMEAL_LIST + categoryId + ":1");
                    cacheClient.delCache(CacheConstants.SETMEAL_LIST + categoryId + ":0");
                });
    }
}
