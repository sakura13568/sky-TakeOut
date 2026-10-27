package com.sky.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sky.constant.CacheConstants;
import com.sky.json.JacksonObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class CacheClient {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final JacksonObjectMapper jacksonObjectMapper = new JacksonObjectMapper();

    private static final String NULL_MARK = "";
    private static final Duration NULL_MARK_TTL = Duration.ofMinutes(2);
    public void addCache(String key, Object value, Duration ttl){
        try {
            String json = jacksonObjectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json,ttl.plusMillis(randomJitterMillis()).toMillis(), TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public <R> List<R> getCache(String key, Class<R> elementType){
        String json = stringRedisTemplate.opsForValue().get(key);
        if(json == null || isMarkNull(json)){
            return null;
        }
        try {
            return jacksonObjectMapper.readValue(json,jacksonObjectMapper.getTypeFactory().constructCollectionType(List.class,elementType));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    public void delCache(String key){
        stringRedisTemplate.delete(key);
    }

    public <R,QP> List<R> queryWithPassThrough(String key, QP queryParam ,Class<R> elementType, Function<QP,List<R>> dbFallback , Duration ttl){
        List<R> cache = getCache(key, elementType);
        if(cache != null){
            return cache;
        }else if (isMarkNull(key)) {
            return null;
        }else{
            List<R> r = dbFallback.apply(queryParam);
            if(r == null){
                 stringRedisTemplate.opsForValue().set(key, NULL_MARK,NULL_MARK_TTL.toMillis(), TimeUnit.MILLISECONDS);
                return null;
            }else{
                addCache(key,r,ttl);
                return r;
            }
        }
    }
    public <T> List<T> queryListWithMutex(String key, Class<T> elementType,
                                          Supplier<List<T>> dbFallback, Duration ttl) {
        List<T> cached = getCache(key, elementType);
        if (cached != null) {
            return cached;
        }
        if (isMarkNull(key)) {
            return null;
        }

        String lockKey = CacheConstants.LOCK_PREFIX + key;
        if (!getLock(lockKey)) {
            // 没抢到锁：短暂等待后重试，或直接查库兜底
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return dbFallback.get();
            }
            return queryListWithMutex(key, elementType, dbFallback, ttl);
        }

        try {
            // 双检：拿到锁后可能别的线程已重建缓存
            cached = getCache(key, elementType);
            if (cached != null) {
                return cached;
            }
            if (isMarkNull(key)) {
                return null;
            }

            List<T> result = dbFallback.get();
            if (result == null) {
                stringRedisTemplate.opsForValue().set(key, NULL_MARK, NULL_MARK_TTL.toMillis(), TimeUnit.MILLISECONDS);
            } else {
                addCache(key, result, ttl);
            }
            return result;
        } finally {
            unLock(lockKey);
        }
    }
    private boolean isMarkNull(String key){
        return NULL_MARK.equals(stringRedisTemplate.opsForValue().get(key));
    }
    private boolean getLock(String lockKey){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey
                , CacheConstants.LOCK_VALUE, CacheConstants.LOCK_EXPIRETIME);
        // 用 Boolean.TRUE.equals(flag) 而不是 flag.equals(...)，避免 setIfAbsent 返回 null 时 NPE
        return Boolean.TRUE.equals(flag);
    }
    private void unLock(String lockKey){
        stringRedisTemplate.delete(lockKey);
    }
    private long randomJitterMillis(){
        return ThreadLocalRandom.current().nextLong(60000);
    }
}
