"""Seed only the application on the isolated Compose network; preserve stock on restart."""

import fcntl
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE_URL = "http://app:8080"
STATE_FILE = Path("/state/product.json")
PRODUCT_NAME = "Row contention load-test product"
INITIAL_STOCK = 1_000_000_000


def api(path, payload=None):
    data = None if payload is None else json.dumps(payload).encode()
    request = urllib.request.Request(
        BASE_URL + path, data=data, headers={"Content-Type": "application/json"}
    )
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if error.code == 404 and payload is None:
            return None
        raise


def wait_for_app(timeout=180):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            health = api("/actuator/health")
            if health and health.get("status") == "UP":
                return
        except (urllib.error.URLError, TimeoutError, OSError):
            pass
        time.sleep(2)
    raise RuntimeError("Application did not become healthy within 180 seconds.")


def save_state(state_file, state):
    temporary = state_file.with_suffix(".tmp")
    temporary.write_text(json.dumps(state) + "\n")
    temporary.replace(state_file)


def seed(state_file=STATE_FILE):
    if state_file.exists():
        state = json.loads(state_file.read_text())
        product_id = state["productId"]
        if type(product_id) is not int or product_id <= 0:
            raise RuntimeError("Invalid saved product ID; reset the test stack with down -v.")
        product = api(f"/products/{product_id}")
        if product is None or product.get("name") != PRODUCT_NAME:
            raise RuntimeError("Saved product does not match the test database; reset with down -v.")
    else:
        product = api("/products", {"name": PRODUCT_NAME, "price": 1.00})
        product_id = product["id"]
        state = {"productId": product_id, "inventoryInitialized": False}
        # Save the ID before the inventory request so a retry can reuse the product.
        save_state(state_file, state)

    inventory = api(f"/inventory/{product_id}")
    if inventory is None:
        if state["inventoryInitialized"]:
            raise RuntimeError("Saved inventory is missing; reset the test stack with down -v.")
        inventory = api("/inventory", {
            "productId": product_id, "availableQuantity": INITIAL_STOCK,
        })
    # Existing inventory must never be topped up, including after an interrupted setup.
    state["inventoryInitialized"] = True
    save_state(state_file, state)
    print("Load-test environment ready", flush=True)
    print("ORDER_URL=http://localhost:8081/orders", flush=True)
    print(f"PRODUCT_ID={product_id}", flush=True)
    print(f"AVAILABLE_QUANTITY={inventory['availableQuantity']}", flush=True)
    print("PostgreSQL: localhost:5433 (database=orderdb, user=orders, password=orders)", flush=True)
    print("Prometheus: http://localhost:9091", flush=True)


def main():
    STATE_FILE.parent.mkdir(parents=True, exist_ok=True)
    # Serialize manual retries against the same state volume.
    with STATE_FILE.with_suffix(".lock").open("w") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        print("Waiting for the load-test application...", flush=True)
        wait_for_app()
        seed()


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"Load-test setup failed: {error}", file=sys.stderr, flush=True)
        sys.exit(1)
