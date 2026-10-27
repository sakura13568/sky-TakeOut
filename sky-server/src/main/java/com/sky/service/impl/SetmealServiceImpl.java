package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.CacheConstants;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.utils.CacheClient;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private CacheClient cacheClient;
    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向套餐表插入数据
        setmealMapper.insert(setmeal);

        //获取生成的套餐id
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });

        //保存套餐和菜品的关联关系
        setmealDishMapper.insertBatch(setmealDishes);

        //缓存失效：新增套餐后，该分类下的套餐列表变了
        //【别删错】这里删的是"套餐列表缓存" SETMEAL_LIST，不是"分类列表缓存" CATEGORY_LIST
        cleanSetmealListCache(Collections.singletonList(setmealDTO.getCategoryId()));
    }
    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum, pageSize);
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }
    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //先收集每个被删套餐的 categoryId，删除后要按分类失效"套餐列表缓存"
        List<Long> categoryIds = new ArrayList<>();
        ids.forEach(id -> {
            Setmeal setmeal = setmealMapper.getById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                //起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
            categoryIds.add(setmeal.getCategoryId());
        });

        ids.forEach(setmealId -> {
            //删除套餐表中的数据
            setmealMapper.deleteById(setmealId);
            //删除套餐菜品关系表中的数据
            setmealDishMapper.deleteBySetmealId(setmealId);
        });

        //缓存失效①：清被删套餐所属分类的"套餐列表缓存" cache:setmeal:list:{categoryId}:{status}
        cleanSetmealListCache(categoryIds);
        //缓存失效②：清每个被删套餐的"套餐内菜品缓存" cache:setmeal:dish:{setmealId}
        cleanSetmealDishCache(ids);
    }
    /**
     * 根据id查询套餐和套餐菜品关系
     *
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        //套餐可能换分类：先查旧分类，换分类后旧分类的"套餐列表缓存"也要清
        Setmeal oldSetmeal = setmealMapper.getById(setmealDTO.getId());
        Long oldCategoryId = oldSetmeal == null ? null : oldSetmeal.getCategoryId();

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //1、修改套餐表，执行update
        setmealMapper.update(setmeal);

        //套餐id
        Long setmealId = setmealDTO.getId();

        //2、删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
        setmealDishMapper.deleteBySetmealId(setmealId);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        //3、重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
        setmealDishMapper.insertBatch(setmealDishes);

        //缓存失效①：旧分类 + 新分类的"套餐列表缓存"都要清（换分类场景）
        List<Long> categoryIds = new ArrayList<>();
        categoryIds.add(oldCategoryId);
        categoryIds.add(setmealDTO.getCategoryId());
        cleanSetmealListCache(categoryIds);
        //缓存失效②：套餐内的菜品关系变了，清该套餐的"套餐内菜品缓存"
        cleanSetmealDishCache(Collections.singletonList(setmealId));
    }
    /**
     * 条件查询
     * @param setmeal
     * @return
     */

    public List<Setmeal> list(Setmeal setmeal) {
        String key = CacheConstants.SETMEAL_LIST + setmeal.getCategoryId() +":" + setmeal.getStatus();
        return cacheClient.queryListWithMutex(key,
                Setmeal.class,
                () -> setmealMapper.list(setmeal),
                CacheConstants.TTL_SHORT);
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */

    public List<DishItemVO> getDishItemById(Long id) {
        String key = CacheConstants.SETMEAL_DISH + id;
        return cacheClient.queryListWithMutex(key,
                DishItemVO.class,
                () -> setmealMapper.getDishItemBySetmealId(id),
                CacheConstants.TTL_MEDIUM);
    }
    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            //select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = ?
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && dishList.size() > 0){
                dishList.forEach(dish -> {
                    if(StatusConstant.DISABLE == dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);

        //缓存失效：套餐起售/停售后，其分类下按 status 过滤的"套餐列表缓存"变了 → 清列表缓存
        //【别删错】这里不用清"套餐内菜品缓存" SETMEAL_DISH：DishItemVO 只有 name/copies/image/description，没有 status，启停不改内容
        Setmeal current = setmealMapper.getById(id);
        if (current != null) {
            cleanSetmealListCache(Collections.singletonList(current.getCategoryId()));
        }
    }

    // ---------- 缓存失效辅助方法 ----------

    /**
     * 失效"套餐列表缓存"：cache:setmeal:list:{categoryId}:{status}
     * 【别删错】这里是 SETMEAL_LIST（按分类查套餐列表），不是 CATEGORY_LIST（分类列表），删错会误清分类缓存。
     */
    private void cleanSetmealListCache(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        categoryIds.stream().filter(Objects::nonNull).distinct().forEach(categoryId -> {
            cacheClient.delCache(CacheConstants.SETMEAL_LIST + categoryId + ":1"); // 起售状态的套餐列表
            cacheClient.delCache(CacheConstants.SETMEAL_LIST + categoryId + ":0"); // 停售状态的套餐列表
        });
    }

    /**
     * 失效"套餐内菜品缓存"：cache:setmeal:dish:{setmealId}
     * 【别删错】入参是【套餐 id】（不是菜品 id）。DishServiceImpl 里同名方法是"用菜品 id 反查套餐 id"，语义不同。
     */
    private void cleanSetmealDishCache(List<Long> setmealIds) {
        if (setmealIds == null || setmealIds.isEmpty()) {
            return;
        }
        setmealIds.stream().filter(Objects::nonNull).distinct().forEach(setmealId -> {
            cacheClient.delCache(CacheConstants.SETMEAL_DISH + setmealId);
        });
    }
}
