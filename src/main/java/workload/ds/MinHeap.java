package workload.ds;

import workload.metrics.Metrics;

/**
 * Array-based binary min-heap of primitive ints (children of i are 2i+1 and 2i+2).
 * Uses the "hole" technique: elements are shifted into the hole and the new
 * element is written once at its final position.
 *
 * Counting rules (done inside the methods):
 *  - steps       : one read of an array cell;
 *  - moves       : one element shifted along the path (or copied during a resize);
 *  - comparisons : one comparison of two elements.
 */
public final class MinHeap {
    private static final int DEFAULT_CAPACITY = 10;

    private int[] a;
    private int size;
    private final Metrics metrics = new Metrics();

    public MinHeap() {
        this(DEFAULT_CAPACITY);
    }

    public MinHeap(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1: " + initialCapacity);
        }
        a = new int[initialCapacity];
    }

    /** Inserts x and restores the heap property by bubbling up. O(log n). */
    public void insert(int x) {
        if (size == a.length) {
            grow();
        }
        int i = size++;
        while (i > 0) {
            int parent = (i - 1) >>> 1;
            metrics.steps++;
            metrics.comparisons++;
            if (a[parent] <= x) {
                break;
            }
            a[i] = a[parent];          // move the larger parent down into the hole
            metrics.moves++;
            i = parent;
        }
        a[i] = x;
    }

    /** Returns the smallest element without removing it. O(1). */
    public int peekMin() {
        if (size == 0) {
            throw new IllegalStateException("heap is empty");
        }
        metrics.steps++;
        return a[0];
    }

    /** Removes and returns the smallest element, restoring the heap by bubbling down. O(log n). */
    public int extractMin() {
        if (size == 0) {
            throw new IllegalStateException("heap is empty");
        }
        metrics.steps++;
        int min = a[0];
        size--;
        if (size > 0) {
            metrics.steps++;
            siftDown(0, a[size]);
        }
        return min;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public Metrics metrics() {
        return metrics;
    }

    /** Test helper (not counted): true if a[parent] <= a[child] for every node. */
    public boolean isHeap() {
        for (int i = 1; i < size; i++) {
            if (a[(i - 1) >>> 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Places x into the subtree rooted at start, whose two child subtrees are already heaps.
     * Cell 'start' is treated as a hole.
     *
     * Loop invariant (see REPORT.md), hole index i: (a) every parent-child pair of the subtree that does not
     * involve cell i satisfies a[parent] <= a[child]; (b) if i != start, then a[parent(i)] < x and
     * a[parent(i)] <= a[c] for both children c of i.
     */
    private void siftDown(int start, int x) {
        int i = start;
        while (true) {
            int c = 2 * i + 1;
            if (c >= size) {
                break;                              // i is a leaf
            }
            metrics.steps++;                        // read left child
            if (c + 1 < size) {
                metrics.steps++;                    // read right child
                metrics.comparisons++;
                if (a[c + 1] < a[c]) {
                    c++;                            // c = index of the smaller child
                }
            }
            metrics.comparisons++;
            if (a[c] >= x) {
                break;                              // x is not larger than both children
            }
            a[i] = a[c];                            // move the smaller child up into the hole
            metrics.moves++;
            i = c;
        }
        a[i] = x;
    }

    private void grow() {
        if (a.length > Integer.MAX_VALUE / 2) {
            throw new OutOfMemoryError("MinHeap is too large");
        }
        int[] bigger = new int[a.length * 2];
        for (int i = 0; i < size; i++) {
            bigger[i] = a[i];
            metrics.moves++;
        }
        a = bigger;
    }

    /**
     * Floyd's bottom-up heap construction in O(n): the input is copied, then siftDown is
     * called for every internal node from the last one down to the root.
     */
    public static MinHeap buildHeap(int[] array) {
        MinHeap heap = new MinHeap(Math.max(1, array.length));
        for (int i = 0; i < array.length; i++) {
            heap.a[i] = array[i];
        }
        heap.size = array.length;
        for (int i = (heap.size >>> 1) - 1; i >= 0; i--) {
            heap.metrics.steps++;                  // read a[i]
            heap.siftDown(i, heap.a[i]);
        }
        return heap;
    }
}
