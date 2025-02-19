package com.debopam.circuitbreaker;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the CircuitBreaker utility.
 */
public class CircuitBreakerTest {

    private CircuitBreaker<String> breaker;

    @BeforeEach
    public void setUp() {
        // Create a breaker that trips after 3 failures with a 2-second recovery timeout and returns a fallback.
        breaker = CircuitBreaker.<String>builder()
                .failureThreshold(3)
                .recoveryTimeout(Duration.ofSeconds(2))
                .fallback(ex -> "Fallback")
                .build();
    }

    @Test
    public void testSuccessfulCallResetsBreaker() throws Exception {
        // A successful call should return the result and keep the breaker closed.
        String result = breaker.execute((Callable<String>) () -> "Success");
        assertEquals("Success", result);
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    @Test
    public void testBreakerTripsAndReturnsFallbackOnOpenState() {
        // Execute three failing calls to trip the breaker.
        Callable<String> failingCall = () -> { throw new RuntimeException("Failure"); };

        for (int i = 0; i < 3; i++) {
            String result;
            try {
                result = breaker.execute(failingCall);
            } catch (Exception ignored) {
                result = null;
            }
            // With fallback defined, the fallback value should be returned even on failure.
            assertEquals("Fallback", result);
        }
        // Breaker should now be OPEN.
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        // When the breaker is open, calling execute immediately returns fallback.
        try {
            String result = breaker.execute((Callable<String>) () -> "Should not execute");
            assertEquals("Fallback", result);
        } catch (Exception e) {
            fail("Fallback should have been returned, not exception: " + e.getMessage());
        }
    }

    @Test
    public void testBreakerRecoveryAfterTimeout() throws Exception {
        // Trip the breaker.
        Callable<String> failingCall = () -> { throw new RuntimeException("Failure"); };
        for (int i = 0; i < 3; i++) {
            try {
                breaker.execute(failingCall);
            } catch (Exception ignored) { }
        }
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        // Wait for recovery timeout to expire.
        Thread.sleep(2100);

        // Now a trial call is allowed (breaker becomes HALF_OPEN).
        // For this test, simulate a successful call.
        String result = breaker.execute((Callable<String>) () -> "Recovered");
        // A successful trial call resets the breaker back to CLOSED.
        assertEquals("Recovered", result);
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }
}
