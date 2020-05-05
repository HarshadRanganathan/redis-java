package com.redislabs.university.RU102J.dao;

import redis.clients.jedis.*;

import java.time.ZonedDateTime;
import com.redislabs.university.RU102J.core.KeyHelper;

public class RateLimiterSlidingDaoRedisImpl implements RateLimiter {

    private final JedisPool jedisPool;
    private final long windowSizeMS;
    private final long maxHits;

    public RateLimiterSlidingDaoRedisImpl(JedisPool pool, long windowSizeMS,
                                          long maxHits) {
        this.jedisPool = pool;
        this.windowSizeMS = windowSizeMS;
        this.maxHits = maxHits;
    }

    // Challenge #7
    @Override
    public void hit(String name) throws RateLimitExceededException {
        try(Jedis jedis = jedisPool.getResource()) {
            String key = getKey(name);
            Transaction transaction = jedis.multi();
            transaction.zadd(key, System.currentTimeMillis(), System.currentTimeMillis() + "-" + Math.random());
            transaction.zremrangeByScore(key, 0, System.currentTimeMillis() - windowSizeMS);
            Response<Long> hits = transaction.zcard(key);
            transaction.exec();
            if (hits.get() > maxHits) {
                throw new RateLimitExceededException();
            }
        }
    }

    private String getKey(String name) {
        return RedisSchema.getSlidingWindowLimiterKey(windowSizeMS, name, maxHits);
    }
}
