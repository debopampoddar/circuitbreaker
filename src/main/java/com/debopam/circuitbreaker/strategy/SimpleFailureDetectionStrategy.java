package com.debopam.circuitbreaker.strategy;

/**
 * A simple failure detection strategy that trips after a fixed threshold.
 */
non-sealed class SimpleFailureDetectionStrategy implements FailureDetectionStrategy {
    private final int threshold;

    public SimpleFailureDetectionStrategy(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean shouldTrip(int failureCount, Throwable lastException) {
        return failureCount >= threshold;
    }
}
