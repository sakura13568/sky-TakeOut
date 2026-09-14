package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;

    /**
     * 条件查询
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    public Result<List<Setmeal>> list(Long categoryId) {
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);

        List<Setmeal> list = setmealService.list(setmeal);
        return Result.success(list);
    }

    /**
     * 根据套餐id查询包含的菜品列表
     *
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }
    //todo 1. DishInSetMealCache 缓存永不失效（最严重）
    //
    //SetmealServiceImpl.java:162 的 getDishItemById 用了 @Cacheable(cacheNames = "DishInSetMealCache", key = "#id")，但全项目没有任何一个地方对它做 @CacheEvict。
    //
    //update()（SetmealServiceImpl.java:126）、deleteBatch()（:86）只 @CacheEvict(cacheNames = "setMealCache", ...)，没清 DishInSetMealCache。结果：
    //
    //- 管理员修改套餐里包含的菜品 → C 端 /user/setmeal/dish/{id} 仍然返回旧的菜品列表。
    //- 管理员修改/删除某个菜品（改了菜品图片、描述、名称，见 SetmealMapper.getDishItemBySetmealId 查的是 d.image / d.description）→ 同样不会清 DishInSetMealCache。
    //
    //2. 菜品停售连带套餐停售，但没清套餐缓存
    //
    //DishServiceImpl.setStatus()（DishServiceImpl.java:126-139）：停售菜品时会把包含它的套餐一起停售（调 setmealMapper.update）。但对应的 DishController.setStatus()（DishController.java:90）只执行了 cleanCache("dish_*")，没清 setMealCache。于是 C 端套餐列表仍显示“起售中”的旧状态。
    //
    //3. 缓存没有 TTL，漏清即永久脏数据
    //
    //- Spring Cache 的 RedisCacheManager 你没配置 entryTtl，默认永不过期；
    //- 手写的 redisTemplate.opsForValue().set(key, dishVOList)（DishServiceImpl.java:169）也没传过期时间。
    //
    //一旦某个失效点漏掉（正是上面 1、2 的情况），脏数据就永远待着。建议加 TTL 兜底。
    //
    //中等问题
    //
    //4. 缓存 key 只用了 categoryId，没包含 status
    //
    //- SetmealServiceImpl.java:151：@Cacheable(key = "#setmeal.categoryId")，但 setmealMapper.list(setmeal) 实际按 categoryId + status 过滤。
    //- DishServiceImpl.java:150：手写 key "dish_" + categoryId，但 dishMapper.list(catego 。
    //
    //目前 C 端只传 status=ENABLE 所以碰巧没问题，但这是隐性雷：只要有人用不同 status 调同 回错误数据。key 应该把 status 也拼进去。
    //
    //5. 菜品和套餐用了两套不一致的缓存机制
    //
    //- 菜品 listWithFlavor 是手写 RedisTemplate；
    //- 套餐是Spring Cache 注解。
    //两者 key 规则、序列化方式都不一样，维护起来容易出错。更关键的是：RedisTemplateConfiguration.java:14-18 只设了 keySerializer（String），value 没设，走默认 JDK 序列化——所以 DishVO/Setmeal/DishItemVO 都被强制要求 Serializable（目前确实都实现了，才没炸）。这也正是你代码里那个 //todo   放java中的list类型 的答案：value 不是 String，是 JDK 序列化后的二进制，所以才能直接放 List。但 JDK 序列化体积大、不可读、有反序列化安全隐患。
    //
    //代码规范问题（低）
    //
    //6. 缓存的清除逻辑写在 Controller 层，且用 keys 命令
    //
    //DishController.java:93-96：
    //
    //private void cleanCache(String patten){
    //    Set keys = redisTemplate.keys(patten);
    //    redisTemplate.delete(keys);
    //}
    //
    //- keys 命令在生产环境会阻塞 Redis（O(N)），应避免；
    //- 缓存读写（listWithFlavor）在 Service，但清除却在 Controller，职责割裂，而且 Controller 直接注入 RedisTemplate 操作缓存（DishController.java:34），不合理。建议把缓存逻辑统一收敛到 Service，用明确的 delete 或 @CacheEvict。
    //
    //7. @CacheEvict 全用 allEntries = true
    //
    //SetmealServiceImpl 的 4 处 evict 都是清整个 setMealCache。功能上安全，但过粗，也会把 命中率。可以按 key = "#setmeal.categoryId" 精确失效（不过要配合第 4 点把 key 补全）。
    //
    //建议的修复方向
    //
    //1. 给 update()、deleteBatch() 增加对 DishInSetMealCache 的失效（精确 key = "#setmeals）。
    //2. DishController.setStatus（或 DishServiceImpl.setStatus）里，停售菜品连带停售套餐时，同步清理 setMealCache 和 DishInSetMealCache。
    //3. 配置 RedisCacheManager（设置 entryTtl 如 30 分钟，并统一用 JSON/Jackson 序列化）
    //4. 缓存 key 补上 status，消除撞 key 隐患。
    //5. 把菜品缓存也统一改成 Spring Cache 注解，或至少把清除逻辑从 Controller 挪回 Servic


}
