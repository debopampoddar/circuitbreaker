package com.debopam;

import com.debopam.circuitbreaker.CircuitBreaker;
import com.debopam.circuitbreaker.CircuitBreakerRegistry;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demonstrates using the CircuitBreaker utility in a multi-threaded environment.
 * <p>
 * This example simulates two remote APIs (API-A and API-B), each with its own CircuitBreaker.
 * When a remote call fails or the breaker is open, the fallback value is returned.
 */
public class ExampleUsage {

    // Registry to maintain circuit breakers for each remote API.
    private static final CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        // Create circuit breakers for two remote APIs with independent configurations.
        CircuitBreaker<String> apiABreaker = registry.getOrCreate("API-A", () ->
                CircuitBreaker.<String>builder()
                        .failureThreshold(3)
                        .recoveryTimeout(Duration.ofSeconds(5))
                        .fallback(ex -> "Fallback-A")
                        .build()
        );

        CircuitBreaker<String> apiBBreaker = registry.getOrCreate("API-B", () ->
                CircuitBreaker.<String>builder()
                        .failureThreshold(2)
                        .recoveryTimeout(Duration.ofSeconds(3))
                        .fallback(ex -> "Fallback-B")
                        .build()
        );

        // Use a thread pool to simulate concurrent calls.
        try(ExecutorService executor = Executors.newFixedThreadPool(4)) {
            // Submit tasks for API-A and API-B.
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    try {
                        String result = apiABreaker.execute((Callable<String>) () -> simulateRemoteCall("API-A"));
                        System.out.println("API-A call result: " + result +
                                " | Breaker state: " + apiABreaker.getState());
                    } catch (Exception e) {
                        System.out.println("API-A call exception: " + e.getMessage());
                    }
                });
                executor.submit(() -> {
                    try {
                        String result = apiBBreaker.execute((Callable<String>) () -> simulateRemoteCall("API-B"));
                        System.out.println("API-B call result: " + result +
                                " | Breaker state: " + apiBBreaker.getState());
                    } catch (Exception e) {
                        System.out.println("API-B call exception: " + e.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Simulates a remote API call that may randomly fail.
     *
     * @param apiName the name of the remote API
     * @return a success message if the call “succeeds”
     * @throws RuntimeException if the call “fails”
     */
    private static String simulateRemoteCall(String apiName) {
        // 70% chance of failure for API-A, 50% for API-B.
        double failureChance = "API-A".equals(apiName) ? 0.7 : 0.5;
        if (RANDOM.nextDouble() < failureChance) {
            throw new RuntimeException("Simulated failure from " + apiName);
        }
        return "Success from " + apiName;
    }
}
