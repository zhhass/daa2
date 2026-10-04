#!/usr/bin/env bash
# One command: compile, run the benchmark (W1-W4 + buildHeap bonus), measure memory, draw the plots.
# Same seed (new Random(42)) => same data and the same counters on every run.
set -euo pipefail
cd "$(dirname "$0")"
mvn -q -DskipTests compile
java -XX:+UseSerialGC -cp target/classes workload.bench.Benchmark results
java -XX:+UseSerialGC -Xmx2g -cp target/classes workload.bench.MemoryFootprint results
python3 scripts/plot.py
