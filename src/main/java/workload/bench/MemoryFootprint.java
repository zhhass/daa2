package workload.bench;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.IntFunction;
import workload.ds.DynamicArray;
import workload.ds.MinHeap;
import workload.ds.MyLinkedList;

/**
 * Bonus Task A: how many bytes do n elements really occupy?
 *
 * Measured as the growth of the used heap (after System.gc()) caused by building the structure;
 * the median of several trials is taken. Run with -XX:+UseSerialGC for stable numbers. The
 * "layout" column is the size computed from the HotSpot object layout (compressed oops, 8-byte
 * alignment): int[] = 16-byte header + 4*capacity; list node = 12 header + 4 value + 4 prev + 4 next = 24.
 *
 * Usage: java -XX:+UseSerialGC -cp target/classes workload.bench.MemoryFootprint [outputDir]
 */
public final class MemoryFootprint {
    static final int[] SIZES = {100, 1_000, 10_000, 100_000, 1_000_000};
    static final int TRIALS = 5;

    static long usedHeap() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 4; i++) {
            System.gc();
        }
        return rt.totalMemory() - rt.freeMemory();
    }

    static long measure(IntFunction<Object> builder, int n) {
        long[] samples = new long[TRIALS];
        for (int t = 0; t < TRIALS; t++) {
            long before = usedHeap();
            Object structure = builder.apply(n);
            long after = usedHeap();
            Reference.reachabilityFence(structure);
            samples[t] = after - before;
        }
        Arrays.sort(samples);
        return samples[TRIALS / 2];
    }

    private static long align8(long bytes) {
        return (bytes + 7) & ~7L;
    }

    /** Capacity reached by a default-constructed (capacity 10) structure after n appends with 2x growth. */
    static int capacityFor(int n) {
        int cap = 10;
        while (cap < n) {
            cap *= 2;
        }
        return cap;
    }

    public static void main(String[] args) throws IOException {
        Path outDir = Paths.get(args.length > 0 ? args[0] : "results");
        Files.createDirectories(outDir);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outDir.resolve("memory.csv"),
                StandardCharsets.UTF_8))) {
            out.println("structure,n,measured_bytes,layout_bytes,measured_mb,bytes_per_element");
            for (int n : SIZES) {
                long arrayLayout = align8(16 + 4L * capacityFor(n));
                long nodeLayout = 24L * n;
                Object[][] cases = {
                    {"DynamicArray", (IntFunction<Object>) k -> {
                        DynamicArray a = new DynamicArray();
                        for (int i = 0; i < k; i++) {
                            a.add(i);
                        }
                        return a;
                    }, arrayLayout},
                    {"MyLinkedList", (IntFunction<Object>) k -> {
                        MyLinkedList l = new MyLinkedList();
                        for (int i = 0; i < k; i++) {
                            l.add(i);
                        }
                        return l;
                    }, nodeLayout},
                    {"MinHeap", (IntFunction<Object>) k -> {
                        MinHeap h = new MinHeap();
                        for (int i = 0; i < k; i++) {
                            h.insert(k - i);
                        }
                        return h;
                    }, arrayLayout},
                };
                for (Object[] c : cases) {
                    @SuppressWarnings("unchecked")
                    long bytes = measure((IntFunction<Object>) c[1], n);
                    out.printf(Locale.ROOT, "%s,%d,%d,%d,%.4f,%.2f%n", c[0], n, bytes, (Long) c[2],
                            bytes / 1048576.0, (double) bytes / n);
                    System.out.printf(Locale.ROOT, "%-13s n=%-8d measured=%10d B  layout=%10d B  (%.1f B/elem)%n",
                            c[0], n, bytes, (Long) c[2], (double) bytes / n);
                }
            }
        }
        System.out.println("Wrote " + outDir.resolve("memory.csv"));
    }
}
