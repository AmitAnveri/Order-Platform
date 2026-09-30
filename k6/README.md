# Row-contention load tests

Run the existing k6 scripts manually against a separate copy of the application,
PostgreSQL, and Prometheus. Orders commit normally so the tests exercise real
inventory row locks. Test data stays in the load-test stack until you reset it.

## Start the test stack

From the repository root, with Docker running:

```sh
docker compose -p order-platform-loadtest -f compose.load-test.yaml up --build -d
docker compose -p order-platform-loadtest -f compose.load-test.yaml logs -f seed
```

Wait for `Load-test environment ready`. The one-time seed container exits with
code 0 after setup. `up -d` may return before seeding finishes; the ready message
is the signal to start testing. If setup fails, inspect the seed and app logs.

The log prints the actual product ID and current available inventory:

```text
Load-test environment ready
ORDER_URL=http://localhost:8081/orders
PRODUCT_ID=<actual-created-id>
AVAILABLE_QUANTITY=1000000000
PostgreSQL: localhost:5433 (database=orderdb, user=orders, password=orders)
Prometheus: http://localhost:9091
```

| Service | Load-test address | Development address |
| --- | --- | --- |
| Application | http://localhost:8081 | http://localhost:8080 |
| PostgreSQL | localhost:5433 | localhost:5432 |
| Prometheus | http://localhost:9091 | http://localhost:9090 |

PostgreSQL uses database `orderdb`, username `orders`, and password `orders` in
both stacks, but the servers and data volumes are separate. The test stack uses
the same application Dockerfile, PostgreSQL and Prometheus image versions,
application database settings, and one-second scrape configuration as development.
Prometheus in the test network scrapes the test app at `app:8080`.

Use this Compose file on its own, with the project name above; do not merge it
with `compose.yaml`. It allocates its own network, `test_pgdata`, `test_promdata`,
and `seed_state` volumes under that project. No development volumes are mounted.
Ports 8081, 5433, and 9091 must be available.

## Product setup and restarts

The seed service waits for the app's health endpoint, creates one product through
`POST /products`, then sets its inventory to **1,000,000,000 units** through
`POST /inventory`. That quantity is a single integer on one inventory row.

The returned product ID is stored in the test-only `seed_state` volume. Subsequent
starts reuse that product and preserve its remaining inventory, including zero
stock. Re-running setup does not refill stock. The seed container performs no
background work once setup completes.

To display connection details again, including the current inventory, run:

```sh
docker compose -p order-platform-loadtest -f compose.load-test.yaml run --rm seed
```

If saved state no longer matches the database, setup fails with reset instructions.
Manage the database and seed-state volumes together using the reset command below.

## Run the scripts yourself

Install k6 locally. Replace `42` below with the product ID from the seed logs:

```sh
export ORDER_URL=http://localhost:8081/orders
export PRODUCT_ID=42

# Burst: 100 concurrent users, one order per user.
k6 run -e ORDER_URL="$ORDER_URL" -e PRODUCT_ID="$PRODUCT_ID" \
  -e VUS=100 k6/hotproduct.js

# Sustained: 20 users continuously create orders for 30 seconds.
k6 run -e ORDER_URL="$ORDER_URL" -e PRODUCT_ID="$PRODUCT_ID" \
  -e VUS=20 k6/sustained-hot-product.js
```

Every user targets the same product, creating contention on its inventory row.
Use more than one VU to exercise contention; a one-VU run provides a baseline.
Both scripts require every order response to be HTTP `201` and return a nonzero
exit code if any order check fails. A `409` means stock was unavailable and makes
the run fail. The stack does not run k6 or clean itself up automatically.

Orders, items, simulated payments, and consumed inventory remain between runs.
For comparable fresh runs, reset the stack between them. Use a consistent warm-up
if comparing steady-state performance; startup and product seeding are not warm-up.
View metrics at http://localhost:9091 and target status at
http://localhost:9091/targets. Running both stacks simultaneously also shares
host CPU, memory, and disk, which can influence measurements.

## Stop or reset

Stop and remove containers while retaining test data:

```sh
docker compose -p order-platform-loadtest -f compose.load-test.yaml down
```

Delete the test database, Prometheus history, and saved seed state:

```sh
docker compose -p order-platform-loadtest -f compose.load-test.yaml down -v
```

Run the startup command again to get a fresh database and seeded product. Read the
new product ID from the logs. These commands target only `order-platform-loadtest`;
the development stack remains separate. Save any desired measurements before reset.

## Check the seed script

The seed container uses Python's standard library with no installed dependencies.
Its regression checks cover initial setup, restart without restocking, retry after
an inventory request failure, and stale saved state:

```sh
python3 -B -m unittest discover -s scripts -p 'test_seed_load_test.py' -v
```
