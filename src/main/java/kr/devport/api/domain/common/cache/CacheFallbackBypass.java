package kr.devport.api.domain.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.stereotype.Component;

/**
 * SpEL-friendly bypass policy for scoped uncertainty state.
 * 
 * Provides cache bypass decisions based on scope uncertainty.
 * Used in @Cacheable condition/unless expressions to fail safe:
 * - Uncertain scope → bypass=true (read-through to source)
 * - Stable scope → bypass=false (cache allowed)
 * 
 * Example usage in @Cacheable:
 * <pre>
 * {@code
 * @Cacheable(
 *   value = CacheNames.ARTICLES,
 *   key = "...",
 *   unless = "@cacheFallbackBypass.shouldBypass('ARTICLE')"
 * )
 * }
 * </pre>
 */
@Component("cacheFallbackBypass")
@RequiredArgsConstructor
@Slf4j
@ImportRuntimeHints(CacheFallbackBypass.CacheFallbackBypassRuntimeHints.class)
public class CacheFallbackBypass {
    
    public static class CacheFallbackBypassRuntimeHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection().registerType(CacheFallbackBypass.class, MemberCategory.INVOKE_PUBLIC_METHODS);
        }
    }
    
    private final CacheFallbackStateStore stateStore;
    
    /**
     * Checks if cache should be bypassed for a given scope name.
     * 
     * Fail-safe policy:
     * - If scope is uncertain → return true (bypass cache, read-through)
     * - If scope is stable → return false (allow cache)
     * 
     * @param scopeName Cache scope name (e.g., "ARTICLE", "GIT_REPO", "LLM")
     * @return true if cache should be bypassed
     */
    public boolean shouldBypass(String scopeName) {
        try {
            CacheScope scope = CacheScope.valueOf(scopeName);
            return shouldBypass(scope);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid scope name: {}, treating as UNKNOWN", scopeName);
            return shouldBypass(CacheScope.UNKNOWN);
        }
    }
    
    /**
     * Checks if cache should be bypassed for a given scope.
     * 
     * @param scope Cache scope to check
     * @return true if cache should be bypassed
     */
    public boolean shouldBypass(CacheScope scope) {
        boolean uncertain = stateStore.isUncertain(scope);
        
        if (uncertain) {
            log.debug("Bypassing cache for scope={} due to uncertainty", scope);
        }
        
        return uncertain;
    }
    
    /**
     * Inverse check for SpEL 'condition' attribute.
     * 
     * @param scopeName Cache scope name
     * @return true if cache is allowed (not uncertain)
     */
    public boolean allowCache(String scopeName) {
        return !shouldBypass(scopeName);
    }
    
    /**
     * Inverse check for SpEL 'condition' attribute.
     * 
     * @param scope Cache scope
     * @return true if cache is allowed (not uncertain)
     */
    public boolean allowCache(CacheScope scope) {
        return !shouldBypass(scope);
    }
}
