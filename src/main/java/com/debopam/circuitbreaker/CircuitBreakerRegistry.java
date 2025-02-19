package com.debopam.circuitbreaker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * A thread-safe registry for managing CircuitBreaker instances.
 * <p>
 * Each remote API call is identified by a unique key, and the registry
 * ensures that the state of each circuit breaker is maintained independently.
 *
 * @author Debopam
 */
public class CircuitBreakerRegistry {

    private final ConcurrentMap<String, CircuitBreaker<?>> registry = new ConcurrentHashMap<>();

    /**
     * Retrieves an existing circuit breaker for the given key or creates one if absent.
     *
     * @param key a unique identifier for the remote API call
     * @param supplier a supplier that creates a new CircuitBreaker if one does not exist
     * @param <T> the type of result produced by the circuit breaker
     * @return the existing or new CircuitBreaker instance
     */
    @SuppressWarnings("unchecked")
    public <T> CircuitBreaker<T> getOrCreate(String key, Supplier<CircuitBreaker<T>> supplier) {
        return (CircuitBreaker<T>) registry.computeIfAbsent(key, k -> supplier.get());
    }
}
