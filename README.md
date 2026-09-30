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

The API is available at `http://localhost:8080` and the Prometheus UI at
`http://localhost:9090`. PostgreSQL is exposed on port `5432`; its data persists
in the Compose `pgdata` volume, and Prometheus' time series in `promdata`.

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

The tests verify that the Spring application context starts and that
`/actuator/info` returns generated build metadata. They use an in-memory H2
database and do not require a running PostgreSQL instance.
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

## Metrics

Spring Boot Actuator exposes four endpoints on the API port:

| Endpoint | Purpose |
| --- | --- |
| `/actuator/health` | Overall status plus per-component detail (`db`, `diskSpace`, `ping`). |
| `/actuator/info` | Build information: application name, artifact, group, version, and build time. |
| `/actuator/prometheus` | Micrometer metrics in Prometheus text format. |
| `/actuator/metrics` | Available meter names; append `/{name}` to inspect a meter. |

Other Actuator endpoints are not exposed over HTTP by the allowlist in
`application.properties`. The `/actuator` discovery endpoint also lists links to
the exposed endpoints. Gradle generates the metadata for `/actuator/info` through
`springBoot.buildInfo()` during builds, including `bootRun`; rebuild/restart the
app to pick up changed build information.

The metrics are produced by Spring Boot's built-in meter binders; no application
code is instrumented by hand. The ones worth watching:

- `http_server_requests_seconds` — request count, latency, and errors, tagged by
  route template (`/orders/{orderId}`, not the literal id), method, and status.
  Histogram buckets are enabled, so `histogram_quantile()` gives real p95/p99.
- `hikaricp_connections_*` — connection pool usage. Relevant because order
  creation takes a pessimistic row lock on inventory, so a slow or contended
  lock shows up here as pending connections before it shows up as user-visible
  timeouts.
- `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`, `jvm_threads_live` — heap,
  garbage collection, and thread counts.
- `logback_events_total` — log volume by level, useful as an error-rate signal.

The Compose stack runs Prometheus, which scrapes the app every 1 second using
`monitoring/prometheus.yml`. Check the target is live at
`http://localhost:9090/targets`, then query from the graph page, for example:

```text
sum by (uri, status) (rate(http_server_requests_seconds_count[1m]))
histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket[5m])))
```

Running the app outside Compose still exposes `/actuator/prometheus`; only the
Prometheus server itself is Compose-provided.

## Row-contention load tests

`k6/hotproduct.js` sends one order per virtual user as a burst;
`k6/sustained-hot-product.js` sends orders continuously for 30 seconds. Both target
one product so concurrent orders contend on the same inventory row. Set `VUS`
above 1 to exercise contention. Both fail the run if any order-creation check
fails (every response must be HTTP `201`).

Start a separate copy of the app, PostgreSQL, and Prometheus for manual testing:

```sh
docker compose -p order-platform-loadtest -f compose.load-test.yaml up --build -d
docker compose -p order-platform-loadtest -f compose.load-test.yaml logs -f seed
```

Wait for `Load-test environment ready`. Setup prints the actual product ID and
seeds one billion inventory units. The test app is at `http://localhost:8081`,
PostgreSQL at `localhost:5433`, and Prometheus at `http://localhost:9091`.
Restarts preserve the product and remaining stock. Run k6 yourself using
`ORDER_URL=http://localhost:8081/orders` and the printed `PRODUCT_ID`.

See [the load-test guide](k6/README.md) for script commands and the manual reset
command. The standalone Compose project uses separate data volumes; test orders
remain there until you delete those volumes. It does not run tests or clean up
automatically.

## Project structure

```text
src/main/java/com/amitanveri/orderplatform/
  domain/       JPA entities and status enums
  repository/   Persistence interfaces and inventory locking
  service/      Business logic and transaction boundaries
  web/          REST controllers, request/response DTOs, error handling
src/main/resources/application.properties
monitoring/     Prometheus scrape configuration
src/test/       Application context test and H2 configuration
gradle/wrapper/ Gradle distribution configuration and wrapper JAR
```

## Current scope

This is a development application. Payments are simulated; there is no payment
provider integration or authentication/authorization — which includes the Actuator
endpoints, so `/actuator/health` and `/actuator/prometheus` are reachable by anyone
who can reach the API port. Hibernate currently updates
the database schema automatically (`ddl-auto=update`). Production deployment
needs managed schema migrations, credential configuration, access control, and
broader API and PostgreSQL concurrency tests. The Compose configuration is intended
for local development.
