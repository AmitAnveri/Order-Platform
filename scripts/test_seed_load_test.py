import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import seed_load_test as seeder


class SeedTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.state = Path(temporary.name) / "product.json"
        self.enterContext(contextlib.redirect_stdout(io.StringIO()))

    def test_fresh_setup_uses_returned_product_id_and_seeds_stock(self):
        with patch.object(seeder, "api", side_effect=[
            {"id": 42}, None, {"availableQuantity": seeder.INITIAL_STOCK},
        ]) as api:
            seeder.seed(self.state)
        self.assertEqual(json.loads(self.state.read_text()), {
            "productId": 42, "inventoryInitialized": True,
        })
        self.assertEqual(api.call_args_list[-1].args, (
            "/inventory", {"productId": 42, "availableQuantity": seeder.INITIAL_STOCK},
        ))

    def test_restart_preserves_consumed_inventory(self):
        self.state.write_text(json.dumps({"productId": 42, "inventoryInitialized": True}))
        with patch.object(seeder, "api", side_effect=[
            {"name": seeder.PRODUCT_NAME}, {"availableQuantity": 7},
        ]) as api:
            seeder.seed(self.state)
        self.assertEqual([call.args for call in api.call_args_list], [
            ("/products/42",), ("/inventory/42",),
        ])

    def test_retry_after_inventory_failure_reuses_product(self):
        with patch.object(seeder, "api", side_effect=[
            {"id": 42}, None, RuntimeError("connection lost"),
        ]):
            with self.assertRaises(RuntimeError):
                seeder.seed(self.state)
        # The failed request might have committed; preserve stock if it now exists.
        with patch.object(seeder, "api", side_effect=[
            {"name": seeder.PRODUCT_NAME}, {"availableQuantity": 9},
        ]) as api:
            seeder.seed(self.state)
        self.assertEqual(api.call_count, 2)
        self.assertTrue(json.loads(self.state.read_text())["inventoryInitialized"])

    def test_stale_state_fails_without_creating_another_product(self):
        self.state.write_text(json.dumps({"productId": 42, "inventoryInitialized": True}))
        with patch.object(seeder, "api", return_value=None) as api:
            with self.assertRaisesRegex(RuntimeError, "reset with down -v"):
                seeder.seed(self.state)
        api.assert_called_once_with("/products/42")


if __name__ == "__main__":
    unittest.main()
