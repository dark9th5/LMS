from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("operations_agent", ROOT / "scripts" / "operations-agent.py")
assert SPEC and SPEC.loader
AGENT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AGENT)


class OperationsAgentTests(unittest.TestCase):
    def test_backup_uses_fixed_repository_script(self) -> None:
        with patch.object(AGENT, "run_command", return_value={"exitCode": 0}) as run:
            success, _ = AGENT.execute({"type": "BACKUP", "parameters": {"ignored": "value"}})
        self.assertTrue(success)
        run.assert_called_once_with(["bash", "scripts/backup.sh"])

    def test_restore_requires_path_and_fixed_confirmation_environment(self) -> None:
        success, result = AGENT.execute({"type": "RESTORE", "parameters": {}})
        self.assertFalse(success)
        self.assertIn("backupPath", result["error"])
        with patch.object(AGENT, "run_command", return_value={"exitCode": 0}) as run:
            success, _ = AGENT.execute({"type": "RESTORE", "parameters": {"backupPath": "backups/20260803-120000"}})
        self.assertTrue(success)
        run.assert_called_once_with(
            ["bash", "scripts/restore.sh", "backups/20260803-120000"],
            {"LMSPILOT_RESTORE_CONFIRMATION": "RESTORE"},
        )

    def test_update_and_rollback_do_not_execute_arbitrary_commands(self) -> None:
        for operation in ("UPDATE", "ROLLBACK"):
            with self.subTest(operation=operation), patch.object(AGENT, "run_command") as run:
                success, result = AGENT.execute({"type": operation, "parameters": {"packagePath": "/tmp/release"}})
                self.assertFalse(success)
                self.assertIn("arbitrary shell execution is disabled", result["error"])
                run.assert_not_called()

    def test_maintenance_accepts_only_on_or_off(self) -> None:
        success, result = AGENT.execute({"type": "MAINTENANCE", "parameters": {"mode": "restart-all"}})
        self.assertFalse(success)
        self.assertIn("ON or OFF", result["error"])


if __name__ == "__main__":
    unittest.main()
