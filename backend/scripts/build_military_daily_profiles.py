#!/usr/bin/env python3
"""Build a compact, deterministic daily military-menu lookup from observations CSV."""

import argparse
import csv
import gzip
import statistics
from collections import defaultdict
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("target", type=Path)
    args = parser.parse_args()

    values: dict[tuple[str, str, str, str, str], list[float]] = defaultdict(list)
    with args.source.open("r", encoding="utf-8-sig", newline="") as source:
        for row in csv.DictReader(source):
            try:
                kcal = float(row["item_kcal"])
            except (KeyError, TypeError, ValueError):
                continue
            if not 0 < kcal <= 2500:
                continue
            key = (
                row["unit_code"].strip(), row["meal_date"].strip(), row["meal_type"].strip(),
                row["search_name"].strip(), row["canonical_name"].strip(),
            )
            if all(key[:4]):
                values[key].append(kcal)

    args.target.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(args.target, "wt", encoding="utf-8", newline="") as target:
        writer = csv.writer(target, lineterminator="\n")
        writer.writerow(["unit_code", "meal_date", "meal_type", "search_name", "canonical_name", "calorie_kcal", "sample_count"])
        for key in sorted(values):
            kcal_values = values[key]
            writer.writerow([*key, f"{statistics.median(kcal_values):.2f}", len(kcal_values)])

    print(f"daily_profiles={len(values)}")
    print(f"saved={args.target}")


if __name__ == "__main__":
    main()
