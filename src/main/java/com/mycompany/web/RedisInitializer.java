package com.mycompany.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisInitializer {
    private final static DateTimeFormatter timef = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private StatefulRedisConnection<String, String> redis;

    public RedisInitializer(ShutdownHooks shutdownHooks) {
        RedisClient client = RedisClient.create(Config.get().redisURI);
        StatefulRedisConnection<String, String> redis = client.connect();
        shutdownHooks.add(() -> client.shutdown());

        redis.sync().set("foo", timef.format(LocalDateTime.now()));

        this.redis = redis;
    }

    public StatefulRedisConnection<String, String> getRedisConnection() {
        return redis;
    }
}
