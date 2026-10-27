package com.sky.constant;

import java.time.Duration;

public class CacheConstants {

    /** 全局缓存前缀 */
    public static final String PREFIX = "cache:";

    /** 互斥锁前缀（防击穿） */
    public static final String LOCK_PREFIX = PREFIX + "lock:";
    public static final String LOCK_VALUE = "1";
    public static final Duration LOCK_EXPIRETIME = Duration.ofSeconds(10L);

    // ---------- key 模板（末尾拼业务 id / 条件） ----------
    /** C端分类列表：cache:category:list:{type} */
    public static final String CATEGORY_LIST = PREFIX + "category:list:";
    /** C端菜品列表：cache:dish:list:{categoryId}:{status} */
    public static final String DISH_LIST = PREFIX + "dish:list:";
    /** C端套餐列表：cache:setmeal:list:{categoryId}:{status} */
    public static final String SETMEAL_LIST = PREFIX + "setmeal:list:";
    /** C端套餐内菜品：cache:setmeal:dish:{setmealId} */
    public static final String SETMEAL_DISH = PREFIX + "setmeal:dish:";
    /** 店铺营业状态（只存 Redis 的常驻 key） */
    public static final String SHOP_STATUS = "shop:status";
    // ---------- TTL（基础 TTL，随机抖动由 CacheClient 统一加） ----------
    public static final Duration TTL_SHORT = Duration.ofMinutes(10);
    public static final Duration TTL_MEDIUM = Duration.ofMinutes(30);
    public static final Duration TTL_LONG = Duration.ofHours(1);
}
