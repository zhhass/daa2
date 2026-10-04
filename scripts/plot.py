#!/usr/bin/env python3
"""Builds the PNG charts in results/plots/ from results/*.csv.  Usage: python3 scripts/plot.py"""
import csv
import os
from collections import defaultdict

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

RES = "results"
OUT = os.path.join(RES, "plots")
os.makedirs(OUT, exist_ok=True)

COLORS = {"DynamicArray": "#1f77b4", "MyLinkedList": "#d62728", "MinHeap": "#2ca02c"}
METRICS = [("steps", "-", "o"), ("moves", "--", "s"), ("comparisons", ":", "^")]


def load(name):
    with open(os.path.join(RES, name), newline="") as f:
        return list(csv.DictReader(f))


def series(rows, workload, variant):
    """-> {structure: sorted [(n, row)]}"""
    out = defaultdict(list)
    for r in rows:
        if r["workload"] == workload and r["variant"] == variant:
            out[r["structure"]].append((int(r["n"]), r))
    return {k: sorted(v, key=lambda t: t[0]) for k, v in out.items()}


def time_panel(ax, data, title):
    for s, pts in data.items():
        ax.plot([n for n, _ in pts], [float(r["time_ms"]) for _, r in pts], marker="o",
                color=COLORS[s], label=s)
    ax.set_xscale("log"); ax.set_yscale("log")
    ax.set_xlabel("n (number of elements)"); ax.set_ylabel("median time (ms)")
    ax.set_title(title, fontsize=10); ax.grid(True, which="both", alpha=0.3); ax.legend()


def counter_panel(ax, data, title):
    for s, pts in data.items():
        for m, ls, mk in METRICS:
            ys = [int(r[m]) for _, r in pts]
            if max(ys) == 0:
                continue                      # an all-zero counter has nothing to draw
            ax.plot([n for n, _ in pts], ys, ls=ls, marker=mk, color=COLORS[s], label=f"{s}: {m}")
    ax.set_xscale("log"); ax.set_yscale("log")
    ax.set_xlabel("n (number of elements)"); ax.set_ylabel("operations (count)")
    ax.set_title(title, fontsize=10); ax.grid(True, which="both", alpha=0.3); ax.legend(fontsize=7)


def one_workload(rows, workload, variants, title, fname):
    fig, axes = plt.subplots(len(variants), 2, figsize=(12, 4.2 * len(variants)), squeeze=False)
    for i, (variant, label) in enumerate(variants):
        data = series(rows, workload, variant)
        time_panel(axes[i][0], data, f"{title}{label}: time vs n")
        counter_panel(axes[i][1], data, f"{title}{label}: operation counts vs n")
    fig.tight_layout()
    fig.savefig(os.path.join(OUT, fname), dpi=130)
    plt.close(fig)


def memory_plot():
    rows = load("memory.csv")
    fig, axes = plt.subplots(1, 2, figsize=(12, 4.2))
    for s in ("DynamicArray", "MyLinkedList", "MinHeap"):
        pts = sorted((int(r["n"]), float(r["measured_mb"]), float(r["bytes_per_element"]))
                     for r in rows if r["structure"] == s)
        style = dict(color=COLORS[s], marker="o")
        if s == "MinHeap":
            style.update(ls="--", marker="x")   # identical to DynamicArray, drawn dashed on top
        axes[0].plot([p[0] for p in pts], [p[1] for p in pts], label=s, **style)
        axes[1].plot([p[0] for p in pts], [p[2] for p in pts], label=s, **style)
    for ax, yl, t in ((axes[0], "memory (MB)", "Memory vs n"),
                      (axes[1], "bytes per element", "Bytes per element vs n")):
        ax.set_xscale("log"); ax.set_xlabel("n (number of elements)"); ax.set_ylabel(yl)
        ax.set_title(t); ax.grid(True, which="both", alpha=0.3); ax.legend()
    axes[0].set_yscale("log")
    fig.tight_layout(); fig.savefig(os.path.join(OUT, "memory.png"), dpi=130); plt.close(fig)


def buildheap_plot():
    rows = load("buildheap.csv")
    fig, axes = plt.subplots(2, 3, figsize=(15, 8), squeeze=False)
    inputs = (("random", "random input"), ("descending", "descending input (worst case for insert)"))
    for r_i, (kind, kind_label) in enumerate(inputs):
        for ax, col, yl in ((axes[r_i][0], "time_ms", "median time (ms)"),
                            (axes[r_i][1], "comparisons", "comparisons"),
                            (axes[r_i][2], "moves", "moves")):
            for variant, c, label in (("insert", "#d62728", "n x insert(x)"),
                                      ("buildHeap", "#2ca02c", "Floyd buildHeap")):
                pts = sorted((int(r["n"]), float(r[col])) for r in rows if r["variant"] == f"{variant}-{kind}")
                ax.plot([p[0] for p in pts], [p[1] for p in pts], marker="o", color=c, label=label)
            ax.set_xscale("log"); ax.set_yscale("log")
            ax.set_xlabel("n (number of elements)"); ax.set_ylabel(yl)
            ax.set_title(f"{yl} vs n, {kind_label}", fontsize=10)
            ax.grid(True, which="both", alpha=0.3); ax.legend()
    fig.tight_layout(); fig.savefig(os.path.join(OUT, "buildheap.png"), dpi=130); plt.close(fig)


if __name__ == "__main__":
    main = load("results.csv")
    one_workload(main, "W1", [("-", "")], "W1 Random access", "w1_random_access.png")
    one_workload(main, "W2", [("-", "")], "W2 Search", "w2_search.png")
    one_workload(main, "W3", [("head", " (head, index 0)"), ("middle", " (middle, index n/2)")],
                 "W3 Insert & remove", "w3_insert_remove.png")
    one_workload(main, "W4", [("-", "")], "W4 Priority processing (MinHeap)", "w4_priority.png")
    if os.path.exists(os.path.join(RES, "memory.csv")):
        memory_plot()
    if os.path.exists(os.path.join(RES, "buildheap.csv")):
        buildheap_plot()
    print("plots written to", OUT)
