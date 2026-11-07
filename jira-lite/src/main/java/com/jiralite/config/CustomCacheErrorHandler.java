package com.jiralite.config;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Resilient cache error handler that suppresses only confirmed Redis connectivity,
 * network, and timeout failures so business operations fall back gracefully to the relational database.
 *
 * Broad Redis exceptions (such as command syntax or script execution errors), serialization
 * defects, and programming bugs are intentionally rethrown to prevent masking application defects.
 */
public class CustomCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        if (!isRedisConnectivityError(exception)) {
            throw exception;
        }
        log.warn("Redis cache GET failed for cache='{}', key='{}'. Falling back to database: {}",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        if (!isRedisConnectivityError(exception)) {
            throw exception;
        }
        log.warn("Redis cache PUT failed for cache='{}', key='{}'. Cache entry skipped: {}",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        if (!isRedisConnectivityError(exception)) {
            throw exception;
        }
        log.warn("Redis cache EVICT failed for cache='{}', key='{}'. Entry may remain stale until TTL expires: {}",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        if (!isRedisConnectivityError(exception)) {
            throw exception;
        }
        log.warn("Redis cache CLEAR failed for cache='{}'. Entries may remain stale until TTL expires: {}",
                cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    /**
     * Determines whether an exception is a confirmed Redis connectivity, network,
     * or timeout failure, as opposed to serialization, programming, or command execution defects.
     */
    private boolean isRedisConnectivityError(RuntimeException exception) {
        if (exception == null) {
            return false;
        }

        // Serialization and explicit data/code defects must always surface
        if (isSerializationOrProgrammingError(exception)) {
            return false;
        }

        // Check exception and cause chain for confirmed connectivity or timeout failures
        Throwable current = exception;
        while (current != null) {
            if (isSerializationOrProgrammingError(current)) {
                return false;
            }
            if (isConfirmedConnectivityOrTimeout(current)) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    private boolean isConfirmedConnectivityOrTimeout(Throwable throwable) {
        return throwable instanceof RedisConnectionFailureException
                || throwable instanceof QueryTimeoutException
                || throwable instanceof DataAccessResourceFailureException
                || throwable instanceof RedisConnectionException
                || throwable instanceof RedisCommandTimeoutException
                || throwable instanceof java.net.ConnectException
                || throwable instanceof java.net.SocketException
                || throwable instanceof java.net.SocketTimeoutException
                || throwable instanceof java.net.UnknownHostException
                || throwable instanceof java.util.concurrent.TimeoutException;
    }

    private boolean isSerializationOrProgrammingError(Throwable throwable) {
        return throwable instanceof SerializationException
                || throwable instanceof com.fasterxml.jackson.core.JsonProcessingException
                || throwable instanceof NullPointerException
                || throwable instanceof IllegalArgumentException
                || throwable instanceof IllegalStateException
                || throwable instanceof ClassCastException;
    }
}
