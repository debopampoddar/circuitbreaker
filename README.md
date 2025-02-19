# Circuit Breaker

A lightweight Circuit Breaker implementation in Java 21 using a fluent API.

This project demonstrates how to implement a lightweight circuit breaker pattern in Java. The implementation supports:
- Independent state management for remote API calls (using a registry)
- A fluent builder API for easy configuration of failure thresholds, recovery timeouts, and fallback functions
- Fallback functionality to immediately return safe default values when the circuit is open
- Multi-threaded usage scenarios

## Project Structure
```
circuitbreaker/
├── pom.xml
└── src
├── main
│   ├── java
│   │   └── com
│   │       └── debopam
│   │           └── circuitbreaker
│   │               ├── CircuitBreaker.java
│   │               ├── CircuitBreakerRegistry.java
│   │               └── ExampleUsage.java
│   └── resources
└── test
├── java
│   └── com
│       └── debopam
│           └── circuitbreaker
│               └── CircuitBreakerTest.java
└── resources
```

## Features

- **Fluent API:** Configure your circuit breaker using a builder pattern.
- **Independent State Management:** Maintain separate circuit breaker states per remote API using a registry.
- **Fallback Functionality:** If the circuit is open or a call fails, the breaker returns a fallback value.
- **Multi-threaded Support:** Demonstrated with an `ExampleUsage` class simulating concurrent remote calls.

## Getting Started

### Prerequisites

- Java 21
- Maven

### Building the Project

From the project root, run:

```sh
mvn clean package
```
This command will compile the project and package it into a JAR file.

### Running the Example
After building the project, you can run the example usage which demonstrates the circuit breaker in action:

```sh
mvn exec:java -Dexec.mainClass="com.debopam.circuitbreaker.ExampleUsage"
```

You should see console output displaying results from simulated remote API calls along with the current state of the circuit breakers.

### Running Tests
To run the unit tests, execute:

```sh
mvn test
```

The tests validate:

Successful calls reset the breaker.
Failing calls trip the breaker and return fallback values.
The breaker recovers after the recovery timeout.

## How It Works
The CircuitBreaker class implements a simple state machine with three states:

- **CLOSED**: Normal operation.
- **OPEN**: Calls are immediately short-circuited and a fallback is returned.
- **HALF_OPEN**: A trial call is allowed to determine if the service has recovered.
A fluent builder API allows you to configure parameters like failure thresholds, recovery timeouts, and fallback functions. The CircuitBreakerRegistry manages independent circuit breaker instances for different remote API calls.

## Example Usage
The ExampleUsage class simulates two remote APIs (API-A and API-B) with independent circuit breakers. It uses a thread pool to mimic concurrent remote calls. When failures occur, the fallback function returns a safe value, keeping the application responsive.
