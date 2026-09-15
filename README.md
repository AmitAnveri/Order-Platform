# Order Platform

A REST API for managing products, inventory, and orders. Built with Java 25,
Spring Boot 4.1.1, Spring Data JPA, and PostgreSQL. The Gradle wrapper is included.

## Features

- Create and retrieve products.
- Set and retrieve available inventory for each product.
- Create orders with price snapshots and aggregated quantities for repeated products.
- Reserve inventory and record a simulated successful payment in one transaction.
- Retrieve orders with their items and payment status.
- Validate requests and return structured errors for invalid input, missing records,
  and insufficient stock.

## Run with Docker

Install Docker with Docker Compose, then run:

```sh
git clone git@github.com:AmitAnveri/Order-Platform.git
cd Order-Platform
docker compose up --build
```

The API is available at `http://localhost:8080`. PostgreSQL is exposed on port
`5432`; its data persists in the Compose `pgdata` volume.

Stop the services with `docker compose down`. This retains the database volume.

## Run locally

Install JDK 25 and Docker with Docker Compose. From the project directory:

```sh
docker compose up -d db
./gradlew bootRun
```

On Windows, use `gradlew.bat` in place of `./gradlew`.

The development database is `orderdb`, with username `orders` and password
`orders`. These are local development defaults. For another database, set
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD` in the application's environment. The Compose file
sets its own database configuration; update that configuration when changing
the Compose database credentials.

Keep real credentials outside Git. `.env` files and local profile configuration
files are ignored. Local `bootRun` does not automatically load a `.env` file;
export environment variables in your shell or configure them in your IDE.

## Build and test

```sh
./gradlew clean build
```

The existing test verifies that the Spring application context starts, using an
in-memory H2 database. It does not require a running PostgreSQL instance.
Build output and test reports are written to `build/` and excluded from Git.
The executable application JAR is generated in `build/libs/`.

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/products` | Create a product with `name` and positive `price` |
| GET | `/products/{id}` | Retrieve a product |
| POST | `/inventory` | Set a product's absolute `availableQuantity` |
| GET | `/inventory/{productId}` | Retrieve available stock |
| POST | `/orders` | Create an order with `userId` and `items` |
| GET | `/orders/{orderId}` | Retrieve an order and its payment |

Example flow (replace product ID `1` with the ID returned by the first request):

```sh
curl -i http://localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard","price":49.99}'

curl -i http://localhost:8080/inventory \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"availableQuantity":10}'

curl -i http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"userId":"demo-user","items":[{"productId":1,"quantity":2}]}'

curl -i http://localhost:8080/orders/1
```

Product and order creation return HTTP `201`. Validation errors return `400`,
missing records return `404`, and insufficient stock returns `409`. Handled
errors use the JSON shape `{"error":"code","message":"details"}`.

## Project structure

```text
src/main/java/com/amitanveri/orderplatform/
  domain/       JPA entities and status enums
  repository/   Persistence interfaces and inventory locking
  service/      Business logic and transaction boundaries
  web/          REST controllers, request/response DTOs, error handling
src/main/resources/application.properties
src/test/       Application context test and H2 configuration
gradle/wrapper/ Gradle distribution configuration and wrapper JAR
```

## Current scope

This is a development application. Payments are simulated; there is no payment
provider integration or authentication/authorization. Hibernate currently updates
the database schema automatically (`ddl-auto=update`). Production deployment
needs managed schema migrations, credential configuration, access control, and
broader API and PostgreSQL concurrency tests. The Compose configuration is intended
for local development.
