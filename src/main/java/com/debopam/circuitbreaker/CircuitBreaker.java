package com.debopam.circuitbreaker;

import com.debopam.circuitbreaker.strategy.FailureDetectionStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A generic CircuitBreaker that protects calls to remote APIs.
 * <p>
 * It supports a fluent API for configuration, independent state transitions,
 * and an optional fallback function that is invoked when the circuit is open
 * (or when a call fails).
 *
 * @param <T> the type of result returned by the protected call
 */
public class CircuitBreaker<T> {

    /**
     * The state of the circuit breaker.
     */
    public enum State { CLOSED, OPEN, HALF_OPEN }

    private volatile State state;
    private final AtomicInteger failureCount;
    private final int failureThreshold;
    private final Duration recoveryTimeout;
    private final FailureDetectionStrategy failureStrategy;
    private volatile Instant lastFailureTime;
    private final Function<Throwable, T> fallbackFunction;

    private CircuitBreaker(int failureThreshold,
                           Duration recoveryTimeout,
                           FailureDetectionStrategy failureStrategy,
                           Function<Throwable, T> fallbackFunction) {
        this.failureThreshold = failureThreshold;
        this.recoveryTimeout = recoveryTimeout;
        this.failureStrategy = failureStrategy;
        this.fallbackFunction = fallbackFunction;
        this.state = State.CLOSED;
        this.failureCount = new AtomicInteger(0);
        this.lastFailureTime = Instant.MIN;
    }

    /**
     * Executes the given callable within the circuit breaker context.
     * <p>
     * If the circuit is OPEN and the recovery timeout has not yet passed, the fallback
     * function is immediately returned (if defined). Otherwise, a trial call is allowed.
     * In case of an exception during the call, the fallback is applied if provided.
     *
     * @param callable the operation to execute
     * @return the result of the callable, or the fallback value if applicable
     * @throws Exception if the call fails and no fallback is defined
     */
    public T execute(Callable<T> callable) throws Exception {
        synchronized (this) {
            if (state == State.OPEN) {
                if (Duration.between(lastFailureTime, Instant.now()).compareTo(recoveryTimeout) < 0) {
                    // Circuit is open and recovery timeout not passed: use fallback if available.
                    if (fallbackFunction != null) {
                        return fallbackFunction.apply(new IllegalStateException("Circuit breaker is open"));
                    }
                    throw new IllegalStateException("Circuit breaker is open");
                } else {
                    // Allow a trial call; transition to HALF_OPEN.
                    state = State.HALF_OPEN;
                }
            }
        }
        try {
            T result = callable.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure(e);
            if (fallbackFunction != null) {
                return fallbackFunction.apply(e);
            }
            throw e;
        }
    }

    /**
     * Overloaded execute method to support a Supplier (which does not throw checked exceptions).
     *
     * @param supplier the supplier to execute
     * @return the result from the supplier or the fallback value if defined
     */
    public T execute(Supplier<T> supplier) {
        try {
            return execute((Callable<T>) () -> supplier.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a decorated version of the given Callable.
     *
     * @param callable the original callable
     * @return a new callable protected by this circuit breaker
     */
    public Callable<T> decorate(Callable<T> callable) {
        return () -> execute(callable);
    }

    private synchronized void onSuccess() {
        if (state == State.HALF_OPEN) {
            reset();
        } else if (state == State.CLOSED) {
            failureCount.set(0);
        }
    }

    private synchronized void onFailure(Exception e) {
        int currentFailures = failureCount.incrementAndGet();
        lastFailureTime = Instant.now();
        if (failureStrategy.shouldTrip(currentFailures, e)) {
            state = State.OPEN;
        }
    }

    private synchronized void reset() {
        failureCount.set(0);
        state = State.CLOSED;
    }

    /**
     * Returns the current state of the circuit breaker.
     *
     * @return the current state (CLOSED, OPEN, or HALF_OPEN)
     */
    public State getState() {
        return state;
    }

    /**
     * Creates a new builder for a CircuitBreaker.
     *
     * @param <T> the type of result produced by the circuit breaker
     * @return a new Builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder for constructing a CircuitBreaker with a fluent API.
     *
     * @param <T> the type of result produced by the circuit breaker
     */
    public static class Builder<T> {
        private int failureThreshold = 3;
        private Duration recoveryTimeout = Duration.ofSeconds(5);
        private FailureDetectionStrategy failureStrategy =
                FailureDetectionStrategy.simpleFailureDetectionStrategy(failureThreshold);
        private Function<Throwable, T> fallbackFunction = null;

        /**
         * Sets the failure threshold (number of consecutive failures required to trip the breaker).
         *
         * @param threshold the failure threshold
         * @return the builder instance
         */
        public Builder<T> failureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }

        /**
         * Sets the recovery timeout (duration to wait before allowing a trial call).
         *
         * @param timeout the recovery timeout
         * @return the builder instance
         */
        public Builder<T> recoveryTimeout(Duration timeout) {
            this.recoveryTimeout = timeout;
            return this;
        }

        /**
         * Sets a custom failure detection strategy.
         *
         * @param strategy the failure detection strategy
         * @return the builder instance
         */
        public Builder<T> failureDetectionStrategy(FailureDetectionStrategy strategy) {
            this.failureStrategy = strategy;
            return this;
        }

        /**
         * Sets a fallback function to be used when the circuit is open or a call fails.
         *
         * @param fallback a function that accepts the thrown exception and returns a fallback value
         * @return the builder instance
         */
        public Builder<T> fallback(Function<Throwable, T> fallback) {
            this.fallbackFunction = fallback;
            return this;
        }

        /**
         * Builds the CircuitBreaker instance.
         *
         * @return a new CircuitBreaker
         */
        public CircuitBreaker<T> build() {
            return new CircuitBreaker<>(failureThreshold, recoveryTimeout, failureStrategy, fallbackFunction);
        }
    }
}



