package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.impl.DishServiceImpl;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.Reader;
import java.util.List;
import java.util.Set;

@RestController
@Slf4j
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
/**
 * 菜品管理
 */
public class DishController {
    @Autowired
    private DishService dishService;
    //todo reids数据库操作更好的写法
    @Autowired
    private RedisTemplate redisTemplate;
    @PostMapping
    @ApiOperation("新增菜品")
    public Result<String> saveDishWithFlavor(@RequestBody DishDTO dishDTO){
        log.info("新增菜品,{}",dishDTO);
         dishService.saveDishWithFlavor(dishDTO);
         String key = "dish_" + dishDTO.getCategoryId();
         cleanCache(key);
         return Result.success();
    }
    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    public Result<PageResult> pageSearch(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询信息，{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageSearch(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> list = dishService.selectDishItemsByCategroyId(categoryId);
        return Result.success(list);
    }
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result deleteDishWithFlavorBatch(@RequestParam List<Long> ids){
        log.info("要删除的菜品,{}",ids);
        dishService.deleteDishWithFlavorBatch(ids);
        cleanCache("dish_*");
        return Result.success();
    }
    @ApiOperation("根据id查询菜品信息")
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("要查询菜品的id,{}",id);
        DishVO dishVo = dishService.getByIdWithFlavors(id);
        return Result.success(dishVo);
    }
    @ApiOperation("修改菜品信息")
    @PutMapping
    public Result update(@RequestBody DishDTO dishDTO, Reader reader){
        log.info("修改菜品信息：{}",dishDTO);
        dishService.updateWithFlavors(dishDTO);
        cleanCache("dish_*");
        return Result.success();
    }
    @ApiOperation("修改菜品售卖状态")
    @PostMapping("/status/{status}")
    public Result setStatus(@RequestParam Long id,@PathVariable Integer status){
        log.info("修改菜品:{},status:{}",id,status);
        dishService.setStatus(id,status);
        cleanCache("dish_*");
        return Result.success();
    }
    private void cleanCache(String patten){
        Set keys = redisTemplate.keys(patten);
        redisTemplate.delete(keys);
    }
}
