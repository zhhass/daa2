package workload.bench;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;
import workload.ds.DynamicArray;
import workload.ds.IntList;
import workload.ds.MinHeap;
import workload.ds.MyLinkedList;
import workload.metrics.Metrics;

/**
 * Runs workloads W1-W4 for n = 100, 1 000, 10 000, 100 000 and writes
 *   results/results.csv    (workload,variant,structure,n,time_ms,steps,moves,comparisons)
 *   results/buildheap.csv  (bonus: n inserts vs Floyd buildHeap, random and descending input)
 *
 * Reproducible: all data comes from new Random(42). Only the operation phase is timed
 * (building the structure is not). Every case runs WARMUP discarded runs followed by RUNS
 * measured runs; the median time is reported. Counters are deterministic and are taken from
 * the last run, after being reset when the measured phase starts.
 *
 * Usage: java -cp target/classes workload.bench.Benchmark [outputDir]
 */
public final class Benchmark {
    static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    static final int SEED = 42;
    static final int WARMUP = 5;
    static final int RUNS = 5;
    static final int GET_QUERIES = 10_000;       // W1
    static final int CONTAINS_QUERIES = 1_000;   // W2
    static final int UPDATE_OPS = 1_000;         // W3
    static final int VALUE_BOUND = 1_000_000;    // stored values are even numbers < 2*VALUE_BOUND

    /** Written to by every run so the JIT cannot discard the measured work. */
    static volatile long sink;

    /** A prepared (already filled) structure ready to be timed. */
    interface Prepared {
        void run();

        Metrics metrics();
    }

    interface Scenario {
        Prepared prepare();
    }

    record Result(double medianMs, Metrics metrics) {
    }

    // ------------------------------------------------------------------ data

    /** Even values only, so odd numbers are guaranteed to be absent (needed by W2). */
    static int[] data(int n) {
        Random rnd = new Random(SEED);
        int[] d = new int[n];
        for (int i = 0; i < n; i++) {
            d[i] = rnd.nextInt(VALUE_BOUND) * 2;
        }
        return d;
    }

    /** n, n-1, ..., 1: every insert bubbles up to the root (worst case for repeated insert). */
    static int[] descendingData(int n) {
        int[] d = new int[n];
        for (int i = 0; i < n; i++) {
            d[i] = n - i;
        }
        return d;
    }

    static IntList filled(Supplier<IntList> factory, int[] data) {
        IntList list = factory.get();
        for (int v : data) {
            list.add(v);
        }
        list.metrics().reset();                  // fill phase is not measured
        return list;
    }

    // ------------------------------------------------------------- scenarios

    /** W1: n values, then 10 000 get(randomIndex). */
    static Scenario w1(Supplier<IntList> factory, int n) {
        return () -> {
            int[] data = data(n);
            Random rnd = new Random(SEED + 1);
            int[] idx = new int[GET_QUERIES];
            for (int i = 0; i < idx.length; i++) {
                idx[i] = rnd.nextInt(n);
            }
            IntList list = filled(factory, data);
            return new Prepared() {
                public void run() {
                    long s = 0;
                    for (int i : idx) {
                        s += list.get(i);
                    }
                    sink = s;
                }

                public Metrics metrics() {
                    return list.metrics();
                }
            };
        };
    }

    /** W2: 1 000 contains(x); half of the queried values are present, half are absent. */
    static Scenario w2(Supplier<IntList> factory, int n) {
        return () -> {
            int[] data = data(n);
            Random rnd = new Random(SEED + 2);
            int[] q = new int[CONTAINS_QUERIES];
            for (int i = 0; i < q.length; i++) {
                q[i] = (i % 2 == 0) ? data[rnd.nextInt(n)]            // present
                                    : rnd.nextInt(VALUE_BOUND) * 2 + 1; // odd => absent
            }
            IntList list = filled(factory, data);
            return new Prepared() {
                public void run() {
                    int found = 0;
                    for (int x : q) {
                        if (list.contains(x)) {
                            found++;
                        }
                    }
                    sink = found;
                }

                public Metrics metrics() {
                    return list.metrics();
                }
            };
        };
    }

    /** W3: 1 000 insertions, then 1 000 removals, both at the given position (0 or n/2). */
    static Scenario w3(Supplier<IntList> factory, int n, boolean middle) {
        return () -> {
            int[] data = data(n);
            int pos = middle ? n / 2 : 0;
            IntList list = filled(factory, data);
            return new Prepared() {
                public void run() {
                    for (int k = 0; k < UPDATE_OPS; k++) {
                        list.add(pos, k);
                    }
                    long s = 0;
                    for (int k = 0; k < UPDATE_OPS; k++) {
                        s += list.remove(pos);
                    }
                    sink = s;
                }

                public Metrics metrics() {
                    return list.metrics();
                }
            };
        };
    }

    /** W4: insert n values, then extractMin() n times and check the order. */
    static Scenario w4(int n) {
        return () -> {
            int[] data = data(n);
            MinHeap heap = new MinHeap();
            return new Prepared() {
                public void run() {
                    for (int v : data) {
                        heap.insert(v);
                    }
                    int prev = Integer.MIN_VALUE;
                    for (int i = 0; i < data.length; i++) {
                        int v = heap.extractMin();
                        if (v < prev) {
                            throw new AssertionError("extractMin order violated at " + i);
                        }
                        prev = v;
                    }
                    sink = prev;
                }

                public Metrics metrics() {
                    return heap.metrics();
                }
            };
        };
    }

    /** Bonus: n separate insert() calls into a pre-sized heap. */
    static Scenario insertAll(int n, boolean descending) {
        return () -> {
            int[] data = descending ? descendingData(n) : data(n);
            MinHeap heap = new MinHeap(n);
            return new Prepared() {
                public void run() {
                    for (int v : data) {
                        heap.insert(v);
                    }
                    sink = heap.size();
                }

                public Metrics metrics() {
                    return heap.metrics();
                }
            };
        };
    }

    /** Bonus: Floyd's bottom-up buildHeap(int[]). */
    static Scenario floyd(int n, boolean descending) {
        return () -> {
            int[] data = descending ? descendingData(n) : data(n);
            MinHeap[] holder = new MinHeap[1];
            return new Prepared() {
                public void run() {
                    holder[0] = MinHeap.buildHeap(data);
                    sink = holder[0].size();
                }

                public Metrics metrics() {
                    return holder[0].metrics();
                }
            };
        };
    }

    // ------------------------------------------------------------- measuring

    static Result measure(Scenario scenario) {
        double[] times = new double[RUNS];
        Metrics last = null;
        for (int r = 0; r < WARMUP + RUNS; r++) {
            Prepared p = scenario.prepare();
            long t0 = System.nanoTime();
            p.run();
            long t1 = System.nanoTime();
            if (r >= WARMUP) {
                times[r - WARMUP] = (t1 - t0) / 1e6;
            }
            last = p.metrics();
        }
        Arrays.sort(times);
        return new Result(times[RUNS / 2], last);
    }

    private static void row(PrintWriter out, String workload, String variant, String structure, int n, Result r) {
        out.printf(Locale.ROOT, "%s,%s,%s,%d,%.3f,%d,%d,%d%n", workload, variant, structure, n,
                r.medianMs(), r.metrics().steps, r.metrics().moves, r.metrics().comparisons);
        out.flush();
        System.out.printf(Locale.ROOT, "%-3s %-6s %-13s n=%-7d %10.3f ms  steps=%d moves=%d cmp=%d%n",
                workload, variant, structure, n, r.medianMs(), r.metrics().steps, r.metrics().moves,
                r.metrics().comparisons);
    }

    /** Throw-away pass over every scenario so the JIT has compiled all hot paths before measuring. */
    private static void globalWarmUp() {
        for (int n : new int[] {1_000, 10_000}) {
        for (int pass = 0; pass < 3; pass++) {
            for (Supplier<IntList> factory : java.util.List.<Supplier<IntList>>of(DynamicArray::new, MyLinkedList::new)) {
                for (Scenario s : new Scenario[] {w1(factory, n), w2(factory, n), w3(factory, n, false),
                        w3(factory, n, true)}) {
                    Prepared p = s.prepare();
                    p.run();
                }
            }
            w4(n).prepare().run();
            insertAll(n, false).prepare().run();
            floyd(n, false).prepare().run();
            insertAll(n, true).prepare().run();
            floyd(n, true).prepare().run();
        }
        }
    }

    public static void main(String[] args) throws IOException {
        Path outDir = Paths.get(args.length > 0 ? args[0] : "results");
        Files.createDirectories(outDir);
        System.out.println("Warm-up pass ...");
        globalWarmUp();

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outDir.resolve("results.csv"),
                StandardCharsets.UTF_8))) {
            out.println("workload,variant,structure,n,time_ms,steps,moves,comparisons");
            for (int n : SIZES) {
                row(out, "W1", "-", "DynamicArray", n, measure(w1(DynamicArray::new, n)));
                row(out, "W1", "-", "MyLinkedList", n, measure(w1(MyLinkedList::new, n)));
                row(out, "W2", "-", "DynamicArray", n, measure(w2(DynamicArray::new, n)));
                row(out, "W2", "-", "MyLinkedList", n, measure(w2(MyLinkedList::new, n)));
                for (String variant : new String[] {"head", "middle"}) {
                    boolean middle = variant.equals("middle");
                    row(out, "W3", variant, "DynamicArray", n, measure(w3(DynamicArray::new, n, middle)));
                    row(out, "W3", variant, "MyLinkedList", n, measure(w3(MyLinkedList::new, n, middle)));
                }
                row(out, "W4", "-", "MinHeap", n, measure(w4(n)));
            }
        }

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outDir.resolve("buildheap.csv"),
                StandardCharsets.UTF_8))) {
            out.println("workload,variant,structure,n,time_ms,steps,moves,comparisons");
            for (int n : SIZES) {
                row(out, "B", "insert-random", "MinHeap", n, measure(insertAll(n, false)));
                row(out, "B", "buildHeap-random", "MinHeap", n, measure(floyd(n, false)));
                row(out, "B", "insert-descending", "MinHeap", n, measure(insertAll(n, true)));
                row(out, "B", "buildHeap-descending", "MinHeap", n, measure(floyd(n, true)));
            }
        }
        System.out.println("Wrote " + outDir.resolve("results.csv") + " and " + outDir.resolve("buildheap.csv"));
    }
}
