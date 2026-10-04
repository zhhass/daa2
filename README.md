# In-Memory Workload Engine (DAA Assignment 2)

Three data structures written from scratch in Java 17 (no `java.util` collections, `int` storage only):

| Structure | File | Notes |
|---|---|---|
| `DynamicArray` | `src/main/java/workload/ds/DynamicArray.java` | `int[]`, grows 2x when full |
| `MyLinkedList` | `src/main/java/workload/ds/MyLinkedList.java` | doubly linked, walks from the nearer end |
| `MinHeap` | `src/main/java/workload/ds/MinHeap.java` | array-based binary heap + Floyd `buildHeap` (bonus B) |

`DynamicArray` and `MyLinkedList` implement the same `IntList` interface, so one benchmark runs both.
Counters (`steps`, `moves`, `comparisons`) live in `workload.metrics.Metrics` and are incremented **inside** the methods.

## Requirements
JDK 17+, Maven 3.8+, Python 3 with `matplotlib` (only for the plots).

## Build and test
```bash
mvn clean test          # compiles and runs the 44 JUnit 5 tests
```

## Run the benchmark (one command)
```bash
./run.sh                # Linux / macOS / Git Bash
```
It compiles the project, runs W1-W4 and writes `results/results.csv`, writes `results/buildheap.csv` (bonus B) and
`results/memory.csv` (bonus A), then draws `results/plots/*.png`. The same seed gives the same data and the same counters;
only the times vary slightly from run to run.

Step by step (works on any OS):
```bash
mvn -q compile
java -XX:+UseSerialGC -cp target/classes workload.bench.Benchmark results
java -XX:+UseSerialGC -Xmx2g -cp target/classes workload.bench.MemoryFootprint results
python3 scripts/plot.py
```
`mvn -q compile exec:java` also runs the benchmark (the `exec-maven-plugin` is configured in the `pom.xml`).

## Benchmark protocol
* Sizes n = 100, 1 000, 10 000, 100 000; data from `new Random(42)`; all structures get the same data.
* W1: fill, then 10 000 `get(randomIndex)`. W2: 1 000 `contains(x)`, half present and half absent
  (stored values are even, absent queries are odd). W3: 1 000 insertions followed by 1 000 removals at index 0
  (`head`) or `n / 2` (`middle`). W4: `MinHeap`, insert n values, then `extractMin()` n times with a non-decreasing check.
* Every case runs 5 discarded warm-up runs and 5 measured runs; the **median** time is saved. A global warm-up pass over
  all workloads runs first. Only the operation phase is timed (filling the structure is not).
* `results.csv` columns: `workload,variant,structure,n,time_ms,steps,moves,comparisons` (`variant` is `head`/`middle` for W3, `-` otherwise).

## Counting rules
| Counter | DynamicArray / MinHeap | MyLinkedList |
|---|---|---|
| steps | one array cell read | one node visited (start node counts 1, each hop +1) |
| moves | one element shifted, or copied while resizing | one link update (`prev`/`next` of a node, `head`/`tail`) |
| comparisons | one comparison of two elements | one comparison of two elements |

## Project layout
```
src/main/java/workload/{ds,metrics,bench}   structures, counters, Benchmark, MemoryFootprint
src/test/java/workload/ds                   JUnit 5 tests (java.util is used only here, as a reference)
results/results.csv, buildheap.csv, memory.csv
results/plots/                              PNG charts
scripts/plot.py                             chart generator
REPORT.md                                   complexity table, loop-invariant proofs, plots, discussion
```

## Git workflow
`main` holds only working code (tag `v1.0`); work was done on `feature/array`, `feature/list`, `feature/heap`,
`feature/metrics` and merged with `--no-ff`. Commit messages follow `feat(array): ...`, `test(heap): ...`, `docs(report): ...`.

## Optional: cross-check memory with JOL
`MemoryFootprint` measures the heap growth after `System.gc()` and compares it with the HotSpot layout (compressed oops).
To cross-check with [JOL](https://openjdk.org/projects/code-tools/jol/), add `org.openjdk.jol:jol-core` and print
`GraphLayout.parseInstance(structure).totalSize()` for each structure.
