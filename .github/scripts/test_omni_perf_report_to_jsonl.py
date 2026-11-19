#!/usr/bin/env python3
"""No-device tests: fabricated inputs are test vectors, never timing evidence."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("omni-perf-report-to-jsonl.py")


def fixture(timestamp=123456789, reused=1200, replaced=0, file_count=1200):
    return (
        "OMNI_PERF_V1\n"
        f"captured_at_ms={timestamp}\n"
        "app_version=1.0.0-rc1-debug\n"
        "device=TEST_DEVICE_NOT_PHYSICAL\n"
        "sdk=33\n"
        f"files_total={file_count}\n"
        "cold_scan_ms=55000\n"
        "cold_scanner_only_ms=42000\n"
        "cold_index_reconcile_ms=10000\n"
        "cold_index_file_rows_ms=9800\n"
        f"cold_index_rows_reused={reused}\n"
        f"cold_index_rows_replaced={replaced}\n"
        "warm_persistent_hit=true\n"
        "warm_file_count_delta=0\n"
        "emulator_signals_detected=false\n"
        "destructive_actions=false\n"
    )


class PhysicalConverterContract(unittest.TestCase):
    def run_cli(self, docs, **changes):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        root = Path(tmp.name)
        files = []
        for i, value in enumerate(docs):
            p = root / f"input_{i}.txt"
            p.write_text(value)
            files.extend(["--input", str(p)])
        out = root / "output.jsonl"
        args = [
            sys.executable, str(SCRIPT), *files, "--source-sha", "a" * 40,
            "--fixture-id", "confirmed_fixture_1200",
            "--integrity-sha256", "c" * 64, "--output", str(out),
        ]
        for key, value in changes.items():
            flag = "--" + key.replace("_", "-")
            pos = args.index(flag)
            args[pos + 1] = str(value)
        p = subprocess.run(args, text=True, capture_output=True)
        return p, out

    def test_three_independent_readonly_reports_export(self):
        result, path = self.run_cli([fixture(timestamp=1001),
                                     fixture(timestamp=1002),
                                     fixture(timestamp=1003)])
        self.assertEqual(0, result.returncode, result.stderr)
        rows = [json.loads(x) for x in path.read_text().splitlines()]
        self.assertEqual(3, len(rows))
        self.assertEqual(9800, rows[0]["file_rows_ms"])
        self.assertEqual("USER_SUPPLIED_PHYSICAL_DIAGNOSTICS_UNVERIFIED", rows[0]["source_kind"])
        self.assertEqual("sha256:" + "c" * 64, rows[0]["integrity_digest"])

    def test_never_invents_missing_index_timing(self):
        result, path = self.run_cli([fixture().replace("cold_index_file_rows_ms=9800\n", "")])
        self.assertEqual(2, result.returncode)
        self.assertFalse(path.exists())

    def test_rejects_mixed_file_counts_or_reuse_ratios(self):
        result, path = self.run_cli([fixture(timestamp=1001), fixture(timestamp=1002, reused=1199, replaced=1)])
        self.assertEqual(2, result.returncode)
        self.assertFalse(path.exists())

    def test_duplicate_measurement_rejected(self):
        result, path = self.run_cli([fixture(), fixture()])
        self.assertEqual(2, result.returncode)
        self.assertFalse(path.exists())

    def test_emulator_or_missing_integrity_rejected(self):
        result, _ = self.run_cli([fixture().replace("emulator_signals_detected=false", "emulator_signals_detected=true")])
        self.assertEqual(2, result.returncode)
        result, _ = self.run_cli([fixture()], integrity_sha256="example_hash")
        self.assertEqual(2, result.returncode)

    def test_refuses_existing_output_without_overwrite(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            input_path, out = root / "original.txt", root / "existing.jsonl"
            input_path.write_text(fixture())
            out.write_text("KEEP")
            p = subprocess.run([
                sys.executable, str(SCRIPT), "--input", str(input_path),
                "--source-sha", "a" * 40, "--fixture-id", "confirmed_fixture_1200",
                "--integrity-sha256", "c" * 64, "--output", str(out),
            ], capture_output=True)
            self.assertEqual(2, p.returncode)
            self.assertEqual("KEEP", out.read_text())


if __name__ == "__main__":
    unittest.main()
