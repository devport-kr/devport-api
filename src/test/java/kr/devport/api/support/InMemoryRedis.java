package kr.devport.api.support;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** RedisTemplate mock을 Map 기반으로 동작하게 만든다. TTL은 흉내 내지 않으므로 만료는 키를 지워서 표현한다. */
public final class InMemoryRedis {

    private InMemoryRedis() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> backing(RedisTemplate<String, Object> redisTemplate) {
        Map<String, Object> store = new ConcurrentHashMap<>();
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        when(ops.get(anyString())).thenAnswer(invocation -> store.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> store.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(ops).set(anyString(), any(), any(Duration.class));
        when(ops.increment(anyString())).thenAnswer(invocation ->
            store.merge(invocation.getArgument(0), 1L, (current, one) -> (Long) current + 1));

        when(redisTemplate.hasKey(anyString())).thenAnswer(invocation -> store.containsKey(invocation.<String>getArgument(0)));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.delete(anyString())).thenAnswer(invocation -> store.remove(invocation.<String>getArgument(0)) != null);
        when(redisTemplate.delete(anyCollection())).thenAnswer(invocation -> {
            Collection<String> keys = invocation.getArgument(0);
            return keys.stream().filter(key -> store.remove(key) != null).count();
        });
        return store;
    }
}
