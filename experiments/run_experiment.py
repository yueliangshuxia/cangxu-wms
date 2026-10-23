#!/usr/bin/env python3
"""Reproducible slot-assignment experiment for the Cangxu WMS thesis.

The script generates a synthetic order history, assigns 200 SKUs to warehouse
slots with three strategies, simulates picking, and writes the result consumed
by the Vue analytics page and the thesis chapter 6.

Usage:
    python experiments/run_experiment.py
    python experiments/run_experiment.py --orders 12480 --skus 200 --seed 42
"""

from __future__ import annotations

import argparse
import json
import math
import random
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from statistics import mean

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "backend" / "src" / "main" / "resources" / "experiments" / "algorithm-comparison.json"

# Same judgment matrix as backend/src/main/java/com/cangxu/wms/inbound/service/InboundService.java
AHP_MATRIX = [
    [1.00, 1.26, 1.55, 2.00],
    [1.00 / 1.26, 1.00, 1.23, 1.59],
    [1.00 / 1.55, 1.00 / 1.23, 1.00, 1.29],
    [1.00 / 2.00, 1.00 / 1.59, 1.00 / 1.29, 1.00],
]
BENEFIT = [True, True, False, True]  # turnover, correlation, distance (cost), fit

ZONES = list("ABCDE")
ROWS = 4
COLS = 12
CAPACITIES = [60, 120, 180, 240]


@dataclass(frozen=True)
class Slot:
    index: int
    zone: str
    row: int
    col: int
    capacity: int


@dataclass
class Sku:
    index: int
    name: str
    volume: int
    turnover: int
    cluster: int


def build_layout(rng: random.Random) -> list[Slot]:
    slots: list[Slot] = []
    index = 0
    for zi, zone in enumerate(ZONES):
        for row in range(1, ROWS + 1):
            for col in range(1, COLS + 1):
                capacity = rng.choices(CAPACITIES, weights=[2, 4, 3, 1])[0]
                slots.append(Slot(index, zone, row, col, capacity))
                index += 1
    return slots


def build_skus(sku_count: int, rng: random.Random) -> list[Sku]:
    skus: list[Sku] = []
    for index in range(sku_count):
        # A few fast movers, a long tail. Zipf-like turnover is closer to real WMS data.
        turnover = int(8 + 2200 / (index + 8) + rng.uniform(0, 80))
        volume = int(rng.randint(20, 220))
        cluster = index % 8
        skus.append(Sku(index, f"SKU-{index + 1:04d}", volume, turnover, cluster))
    return skus


def build_orders(skus: list[Sku], order_count: int, rng: random.Random) -> list[list[int]]:
    """Generate orders with a long tail and eight correlated product clusters."""
    # Weighted sampling: high-turnover SKUs appear more often, cluster members co-occur.
    weights = [sku.turnover for sku in skus]
    clusters: dict[int, list[int]] = {}
    for sku in skus:
        clusters.setdefault(sku.cluster, []).append(sku.index)

    orders: list[list[int]] = []
    for _ in range(order_count):
        line_count = rng.choices([1, 2, 3, 4, 5, 6], weights=[28, 30, 20, 12, 7, 3])[0]
        chosen: set[int] = set()
        if rng.random() < 0.35:
            cluster = rng.randrange(8)
            members = clusters[cluster]
            chosen.update(rng.sample(members, min(len(members), rng.randint(1, 3))))
        while len(chosen) < line_count:
            chosen.add(rng.choices(range(len(skus)), weights=weights, k=1)[0])
        orders.append(sorted(chosen))
    return orders


def apriori_pairs(orders: list[list[int]], min_support: float) -> dict[tuple[int, int], float]:
    """Simplified Apriori: frequent 2-itemsets and their lift."""
    pair_counts: dict[tuple[int, int], int] = {}
    item_counts: dict[int, int] = {}
    for order in orders:
        for item in order:
            item_counts[item] = item_counts.get(item, 0) + 1
        for i in range(len(order)):
            for j in range(i + 1, len(order)):
                pair = (order[i], order[j])
                pair_counts[pair] = pair_counts.get(pair, 0) + 1

    total = len(orders)
    pairs: dict[tuple[int, int], float] = {}
    for pair, count in pair_counts.items():
        support = count / total
        if support < min_support:
            continue
        a, b = pair
        lift = support / ((item_counts[a] / total) * (item_counts[b] / total))
        if lift > 1.0:
            pairs[pair] = lift
    return pairs


def slot_distance(a: Slot, b: Slot) -> float:
    """Grid distance in metres: zone crossing is expensive, columns are cheap."""
    zone_a, zone_b = ZONES.index(a.zone), ZONES.index(b.zone)
    return abs(zone_a - zone_b) * 20 + abs(a.row - b.row) * 6 + abs(a.col - b.col) * 2


def distance_to_outbound(slot: Slot) -> float:
    return ZONES.index(slot.zone) * 20 + (slot.row - 1) * 6 + (slot.col - 1) * 2


def ahp_weights(matrix: list[list[float]]) -> tuple[list[float], float]:
    size = len(matrix)
    geometric = []
    for row in matrix:
        geometric.append(math.prod(row) ** (1.0 / size))
    total = sum(geometric)
    weights = [value / total for value in geometric]

    # Consistency ratio using the standard random index for n=4.
    weighted = [sum(matrix[i][j] * weights[j] for j in range(size)) for i in range(size)]
    lambda_max = sum(weighted[i] / weights[i] for i in range(size)) / size
    ci = (lambda_max - size) / (size - 1)
    ri = {1: 0.0, 2: 0.0, 3: 0.58, 4: 0.90, 5: 1.12}[size]
    cr = 0.0 if ri == 0 else ci / ri
    return weights, cr


def normalize_matrix(matrix: list[list[float]]) -> list[list[float]]:
    size = len(matrix)
    columns = len(matrix[0])
    result = [[0.0] * columns for _ in range(size)]
    for j in range(columns):
        norm = math.sqrt(sum(matrix[i][j] ** 2 for i in range(size))) or 1.0
        for i in range(size):
            result[i][j] = matrix[i][j] / norm
    return result


def topsis(matrix: list[list[float]], weights: list[float]) -> list[float]:
    normalized = normalize_matrix(matrix)
    weighted = [[normalized[i][j] * weights[j] for j in range(len(weights))]
                for i in range(len(matrix))]
    best = []
    worst = []
    for j, benefit in enumerate(BENEFIT):
        column = [row[j] for row in weighted]
        best.append(max(column) if benefit else min(column))
        worst.append(min(column) if benefit else max(column))

    closeness = []
    for row in weighted:
        d_best = math.sqrt(sum((row[j] - best[j]) ** 2 for j in range(len(row))))
        d_worst = math.sqrt(sum((row[j] - worst[j]) ** 2 for j in range(len(row))))
        closeness.append(d_worst / (d_best + d_worst) if d_best + d_worst else 0.0)
    return closeness


def assign_random(skus: list[Sku], slots: list[Slot], seed: int) -> dict[int, Slot]:
    rng = random.Random(seed)
    shuffled = slots[:]
    rng.shuffle(shuffled)
    return {sku.index: shuffled[i] for i, sku in enumerate(skus)}


def assign_abc(skus: list[Sku], slots: list[Slot]) -> dict[int, Slot]:
    """Classic ABC: A/B/C classes get the nearest 20%/30%/50% of slots.

    Within a class the assignment is shuffled, which is how most ABC warehouses
    operate in practice (the class determines the zone, not the exact slot).
    """
    ordered_skus = sorted(skus, key=lambda sku: sku.turnover, reverse=True)
    ordered_slots = sorted(slots, key=distance_to_outbound)
    a_count = int(len(ordered_skus) * 0.2)
    b_count = int(len(ordered_skus) * 0.3)
    groups = [ordered_skus[:a_count],
              ordered_skus[a_count:a_count + b_count],
              ordered_skus[a_count + b_count:]]
    slot_groups = [ordered_slots[:a_count],
                   ordered_slots[a_count:a_count + b_count],
                   ordered_slots[a_count + b_count:]]
    rng = random.Random(7)
    assignment: dict[int, Slot] = {}
    for sku_group, slot_group in zip(groups, slot_groups):
        sku_group = sku_group[:]
        slot_group = slot_group[:]
        rng.shuffle(sku_group)
        rng.shuffle(slot_group)
        for sku, slot in zip(sku_group, slot_group):
            assignment[sku.index] = slot
    return assignment


def assign_ahp_topsis(skus: list[Sku], slots: list[Slot],
                      pairs: dict[tuple[int, int], float]) -> tuple[dict[int, Slot], float]:
    weights, cr = ahp_weights(AHP_MATRIX)
    ordered_skus = sorted(skus, key=lambda sku: sku.turnover, reverse=True)
    assigned: dict[int, Slot] = {}
    partners: dict[int, list[tuple[int, float]]] = {}
    for (a, b), lift in pairs.items():
        partners.setdefault(a, []).append((b, lift))
        partners.setdefault(b, []).append((a, lift))

    free_slots = slots[:]
    for sku in ordered_skus:
        if not free_slots:
            break
        placed_partners = [(assigned[other], lift)
                           for other, lift in partners.get(sku.index, [])
                           if other in assigned]
        matrix = []
        for slot in free_slots:
            # Correlation is high when associated SKUs already sit in the same zone.
            correlation = sum(lift for partner_slot, lift in placed_partners
                              if partner_slot.zone == slot.zone)
            if placed_partners:
                correlation = correlation / sum(lift for _, lift in placed_partners)
            fit = 1.0 - abs(sku.volume - slot.capacity) / max(sku.volume, slot.capacity)
            matrix.append([
                float(sku.turnover),
                correlation,
                distance_to_outbound(slot),
                fit,
            ])
        scores = topsis(matrix, weights)
        best_index = max(range(len(scores)), key=lambda i: scores[i])
        assigned[sku.index] = free_slots.pop(best_index)
    return assigned, cr


def simulate(orders: list[list[int]], assignment: dict[int, Slot]) -> tuple[float, float, float]:
    """Return average picking distance (m), time (min), and slot fill utilisation (%)."""
    distances = []
    for order in orders:
        stops = []
        seen = set()
        for sku_index in order:
            slot = assignment[sku_index]
            if slot.index not in seen:
                seen.add(slot.index)
                stops.append(slot)
        # Nearest-neighbour route from the outbound station and back.
        current = Slot(-1, "A", 0, 0, 0)
        remaining = stops[:]
        path = 0.0
        while remaining:
            nxt = min(remaining, key=lambda slot: slot_distance(current, slot))
            path += slot_distance(current, nxt)
            current = nxt
            remaining.remove(nxt)
        path += distance_to_outbound(current) if stops else 0.0
        distances.append(path)

    avg_distance = mean(distances) if distances else 0.0
    lines = mean([len(order) for order in orders]) if orders else 0.0
    avg_time = avg_distance / 1.2 / 60.0 + lines * 0.45
    used = {slot.index: slot for slot in assignment.values()}
    fill = sum(min(1.0, capacity_fill(assignment, slot)) for slot in used.values()) / len(used) * 100.0
    return avg_distance, avg_time, fill


def capacity_fill(assignment: dict[int, Slot], slot: Slot) -> float:
    sku = next((sku for sku, assigned in assignment.items() if assigned.index == slot.index), None)
    if sku is None:
        return 0.0
    volume = next((s.volume for s in SKUS if s.index == sku), 0)
    return volume / slot.capacity


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--orders", type=int, default=12480)
    parser.add_argument("--skus", type=int, default=200)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--runs", type=int, default=10)
    args = parser.parse_args()

    rng = random.Random(args.seed)
    global SKUS
    SKUS = build_skus(args.skus, rng)
    slots = build_layout(rng)
    orders = build_orders(SKUS, args.orders, rng)
    pairs = apriori_pairs(orders, min_support=0.0005)

    random_results = [simulate(orders, assign_random(SKUS, slots, args.seed + run))
                      for run in range(args.runs)]
    random_distance = mean(result[0] for result in random_results)
    random_time = mean(result[1] for result in random_results)
    random_fill = mean(result[2] for result in random_results)

    abc_assignment = assign_abc(SKUS, slots)
    abc_distance, abc_time, abc_fill = simulate(orders, abc_assignment)

    ahp_assignment, cr = assign_ahp_topsis(SKUS, slots, pairs)
    ahp_distance, ahp_time, ahp_fill = simulate(orders, ahp_assignment)

    result = {
        "strategies": [
            {"method": "随机上架", "distance": round(random_distance, 1),
             "time": round(random_time, 1), "utilization": round(random_fill, 1)},
            {"method": "ABC 分类", "distance": round(abc_distance, 1),
             "time": round(abc_time, 1), "utilization": round(abc_fill, 1)},
            {"method": "AHP-TOPSIS", "distance": round(ahp_distance, 1),
             "time": round(ahp_time, 1), "utilization": round(ahp_fill, 1)},
        ],
        "distanceImprovement": round((random_distance - ahp_distance) / random_distance * 100, 1),
        "timeImprovement": round((random_time - ahp_time) / random_time * 100, 1),
        "utilizationImprovement": round(ahp_fill - random_fill, 1),
        "consistencyRatio": round(cr, 3),
        "orders": len(orders),
        "skus": len(SKUS),
        "generatedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "source": "experiments/run_experiment.py",
        "seed": args.seed,
        "runs": args.runs,
        "frequentPairs": len(pairs),
        "profile": {
            "zones": len(ZONES), "rows": ROWS, "cols": COLS, "slots": len(slots),
            "walkingSpeedMps": 1.2, "pickMinutesPerLine": 0.45,
        },
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    print(f"\nWrote {OUTPUT}")


if __name__ == "__main__":
    main()
