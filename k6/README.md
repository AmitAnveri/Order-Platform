# Row-contention load tests

Run the k6 scripts manually against the application started with `compose.yaml`.
Orders commit normally so the tests exercise real inventory row locks. Tests use
the development database, and their orders and inventory changes persist.

## Start the stack

From the repository root, with Docker running:

```sh
docker compose up --build -d
curl -fsS http://localhost:8080/actuator/health
```

Wait for the health endpoint to report `UP` before setting up test data. If the
app is still starting, retry the health check; inspect `docker compose logs app`
if it fails to become healthy.

| Service | Address |
| --- | --- |
| Application | http://localhost:8080 |
| PostgreSQL | localhost:5432 |
| Prometheus | http://localhost:9090 |

PostgreSQL uses database `orderdb`, username `orders`, and password `orders`.
The stack stores database data in `pgdata` and Prometheus history in `promdata`.
Prometheus scrapes the app every second at `app:8080` within the Compose network.

## Set up a product

There is no automatic seed service. Create a product through the API:

```sh
curl -i http://localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Load-test product","price":1.00}'
```

Replace `42` below with the product ID returned by that request, then set its
inventory to **1,000,000,000 units**:

```sh
export PRODUCT_ID=42
export ORDER_URL=http://localhost:8080/orders

curl -i http://localhost:8080/inventory \
  -H 'Content-Type: application/json' \
  -d "{\"productId\":${PRODUCT_ID},\"availableQuantity\":1000000000}"
```

Inventory is an absolute quantity on one row. Repeating the inventory request
refills that product; restarting the stack preserves its remaining stock.

## Run the scripts yourself

Install k6 locally, then run from the repository root using the variables above:

```sh
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
Use a consistent warm-up if comparing steady-state performance; startup and
product setup are not warm-up. View metrics at http://localhost:9090 and target
status at http://localhost:9090/targets.

## Stop or reset

Stop and remove containers while retaining data:

```sh
docker compose down
```

For a fresh database, the following command deletes **all data in this Compose
stack**, including development data, test orders, and Prometheus history:

```sh
docker compose down -v
```

Save any desired data and measurements before resetting. Start the stack again
and repeat the product and inventory setup, using the newly returned product ID.
