package com.debopam.circuitbreaker.strategy;

/**
 * Defines a strategy to decide whether the circuit breaker should trip.
 */
public interface FailureDetectionStrategy {
    /**
     * Determines if the failure threshold has been reached.
     *
     * @param failureCount the current number of consecutive failures
     * @param lastException the exception from the last failure
     * @return true if the circuit should open, false otherwise
     */
    boolean shouldTrip(int failureCount, Throwable lastException);

    static SimpleFailureDetectionStrategy simpleFailureDetectionStrategy(int failureThreshold) {
        return new SimpleFailureDetectionStrategy(failureThreshold);
    }
}
