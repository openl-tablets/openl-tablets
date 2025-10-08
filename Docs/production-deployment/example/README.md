# OpenL Rule Services Example

This folder contains an example with several simple OpenL Rule Services that:

- Illustrates how a multi-module OpenL Tablets project can be structured.
- Demonstrates how to containerize and expose the service via Docker.
- Includes an extension for OpenAPI-based authorization using client ID/secret headers.

---

## Running the example

### Requirements

- Java SDK
- Maven
- Docker CLI with the Docker Compose plugin
- A containerization service capable of running Docker containers

### Steps

#### Build the project

```bash
mvn clean package
```

#### Start the containerized OpenL Rule Services

```bash
docker compose up --build
```

Running this setup will:

- Build the Rule Services from `rules.xml`
- Package it with the security extension and OpenAPI configuration
- Start it on port `8080` (with debug port `1077` exposed as `1177`)

---

## Security overview

This project includes a basic OpenAPI-based client authorization mechanism, implemented using custom extensions.

- Uses Basic Authentication
- Registers security schemes via `openapi-configuration.json`
- Enforces checks using Java classes wired through Spring configuration

For detailed documentation, see [Security Extensions](../../Security.md).

---

## Use cases

- Demonstrating header-based client authentication
- Extending OpenL services with custom access control logic
- Serving as a base for production-grade OpenL Tablets Rule Services

---

## What this example demonstrates

- How to define and package rules using OpenL
- How to run the rules as a Docker-based service
- How to integrate client authentication
- How to use custom Java classes and Spring configuration for authorization
- How to make Unit tests for OpenL Rule Services

---

## Available endpoint

After running the container, the Rule Services are available at:

```
http://localhost:8080/
```

To test an endpoint:

```bash
curl -X GET "http://localhost:8080/example-main/Ping"   -H "Authorization: Basic b3BlbmxfdXNlcjpvcGVubF9wYXNzd29yZA==" -v
```

---

## Notes

- This is a sample configuration and should be security-hardened before use in production.
- Debugging is enabled on port `1177`.

---

## Cleanup

To remove all containers and volumes:

```bash
docker compose down -v
```