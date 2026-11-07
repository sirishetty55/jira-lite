package com.jiralite.config;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.serializer.SerializationException;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomCacheErrorHandlerTest {

    private CustomCacheErrorHandler errorHandler;
    private Cache mockCache;

    @BeforeEach
    void setUp() {
        errorHandler = new CustomCacheErrorHandler();
        mockCache = mock(Cache.class);
        when(mockCache.getName()).thenReturn("issues");
    }

    @Test
    @DisplayName("handleCacheGetError: absorbs confirmed RedisConnectionFailureException")
    void handleCacheGetError_AbsorbsConnectionFailureException() {
        RedisConnectionFailureException exception =
                new RedisConnectionFailureException("Cannot connect to Redis on localhost:6379");
        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(exception, mockCache, "issue:10"));
    }

    @Test
    @DisplayName("handleCacheGetError: absorbs Lettuce RedisConnectionException")
    void handleCacheGetError_AbsorbsLettuceConnectionException() {
        RedisConnectionException exception =
                new RedisConnectionException("Unable to connect to redis:6379");
        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(exception, mockCache, "issue:10"));
    }

    @Test
    @DisplayName("handleCacheGetError: rethrows SerializationException so deserialization defects surface")
    void handleCacheGetError_RethrowsSerializationException() {
        SerializationException exception =
                new SerializationException("Could not read JSON: Could not resolve subtype");
        assertThrows(SerializationException.class,
                () -> errorHandler.handleCacheGetError(exception, mockCache, "issue:10"));
    }

    @Test
    @DisplayName("handleCachePutError: absorbs confirmed QueryTimeoutException")
    void handleCachePutError_AbsorbsTimeoutException() {
        QueryTimeoutException exception = new QueryTimeoutException("Redis command timed out after 2000ms");
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(exception, mockCache, "issue:10", "payload"));
    }

    @Test
    @DisplayName("handleCachePutError: absorbs Lettuce RedisCommandTimeoutException")
    void handleCachePutError_AbsorbsLettuceTimeoutException() {
        RedisCommandTimeoutException exception = new RedisCommandTimeoutException("Command timed out");
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(exception, mockCache, "issue:10", "payload"));
    }

    @Test
    @DisplayName("handleCachePutError: rethrows broad RedisCommandExecutionException (e.g. syntax / WRONGTYPE)")
    void handleCachePutError_RethrowsCommandExecutionException() {
        RedisCommandExecutionException exception =
                new RedisCommandExecutionException("WRONGTYPE Operation against a key holding the wrong kind of value");
        assertThrows(RedisCommandExecutionException.class,
                () -> errorHandler.handleCachePutError(exception, mockCache, "issue:10", "payload"));
    }

    @Test
    @DisplayName("handleCachePutError: rethrows broad RedisException with no network cause")
    void handleCachePutError_RethrowsGenericRedisException() {
        RedisException exception = new RedisException("Unexpected Redis state transition");
        assertThrows(RedisException.class,
                () -> errorHandler.handleCachePutError(exception, mockCache, "issue:10", "payload"));
    }

    @Test
    @DisplayName("handleCachePutError: rethrows broad RedisSystemException with no network cause")
    void handleCachePutError_RethrowsGenericRedisSystemException() {
        RedisSystemException exception = new RedisSystemException("Redis internal error", null);
        assertThrows(RedisSystemException.class,
                () -> errorHandler.handleCachePutError(exception, mockCache, "issue:10", "payload"));
    }

    @Test
    @DisplayName("handleCachePutError: absorbs RedisSystemException when caused by network ConnectException")
    void handleCachePutError_AbsorbsRedisSystemExceptionWithNetworkCause() {
        RedisSystemException exception = new RedisSystemException("Redis unreachable",
                new ConnectException("Connection refused: no further information"));
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(exception, mockCache, "issue:10", "payload"));
    }

    @Test
    @DisplayName("handleCachePutError: rethrows NullPointerException programming errors")
    void handleCachePutError_RethrowsNullPointerException() {
        NullPointerException exception = new NullPointerException("Null key encountered in custom code");
        assertThrows(NullPointerException.class,
                () -> errorHandler.handleCachePutError(exception, mockCache, "issue:10", "payload"));
    }

    @Test
    @DisplayName("handleCacheEvictError: absorbs confirmed RedisConnectionFailureException")
    void handleCacheEvictError_AbsorbsConnectionException() {
        RedisConnectionFailureException exception =
                new RedisConnectionFailureException("Redis connection timed out");
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(exception, mockCache, "issue:10"));
    }

    @Test
    @DisplayName("handleCacheEvictError: rethrows IllegalArgumentException")
    void handleCacheEvictError_RethrowsIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid key argument format");
        assertThrows(IllegalArgumentException.class,
                () -> errorHandler.handleCacheEvictError(exception, mockCache, "issue:10"));
    }

    @Test
    @DisplayName("handleCacheClearError: absorbs RedisSystemException with confirmed socket cause")
    void handleCacheClearError_AbsorbsSystemExceptionWithSocketCause() {
        RedisSystemException exception = new RedisSystemException("Socket failure",
                new java.net.SocketException("Broken pipe"));
        assertDoesNotThrow(() -> errorHandler.handleCacheClearError(exception, mockCache));
    }

    @Test
    @DisplayName("handleCacheClearError: rethrows RedisSystemException wrapping RedisCommandExecutionException")
    void handleCacheClearError_RethrowsSystemExceptionWithCommandCause() {
        RedisSystemException exception = new RedisSystemException("Command failed",
                new RedisCommandExecutionException("ERR unknown command 'FLUSHALL'"));
        assertThrows(RedisSystemException.class,
                () -> errorHandler.handleCacheClearError(exception, mockCache));
    }

    @Test
    @DisplayName("Null-safety: handles null cache references gracefully for connectivity errors")
    void nullSafety_HandlesNullCache() {
        RedisConnectionFailureException exception =
                new RedisConnectionFailureException("Generic connection error");
        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(exception, null, "key"));
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(exception, null, "key", "val"));
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(exception, null, "key"));
        assertDoesNotThrow(() -> errorHandler.handleCacheClearError(exception, null));
    }
}
