package workload.ds;

import workload.metrics.Metrics;

/**
 * Growable array of primitive ints written from scratch (no java.util collections).
 * The backing int[] doubles when it is full, which gives amortized O(1) add(x).
 *
 * Counting rules (done inside the methods):
 *  - steps       : one read of an array cell (get, remove's read, every cell inspected by contains);
 *  - moves       : one element shifted by add(index,x)/remove(index) or copied during a resize;
 *  - comparisons : one comparison of two elements (contains).
 * The final write of the newly inserted value is not a "move" (nothing is shifted by it).
 */
public final class DynamicArray implements IntList {
    private static final int DEFAULT_CAPACITY = 10;

    private int[] data;
    private int size;
    private final Metrics metrics = new Metrics();

    public DynamicArray() {
        this(DEFAULT_CAPACITY);
    }

    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1: " + initialCapacity);
        }
        data = new int[initialCapacity];
    }

    @Override
    public void add(int x) {
        if (size == data.length) {
            grow();
        }
        data[size++] = x;
    }

    @Override
    public void add(int index, int x) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
        if (size == data.length) {
            grow();
        }
        for (int i = size; i > index; i--) {   // shift the tail one cell to the right
            data[i] = data[i - 1];
            metrics.moves++;
        }
        data[index] = x;
        size++;
    }

    @Override
    public int remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
        metrics.steps++;
        int removed = data[index];
        for (int i = index; i < size - 1; i++) {   // shift the tail one cell to the left
            data[i] = data[i + 1];
            metrics.moves++;
        }
        size--;
        return removed;
    }

    @Override
    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
        metrics.steps++;
        return data[index];
    }

    @Override
    public boolean contains(int x) {
        // Loop invariant (see REPORT.md): before each iteration x does not occur in data[0..i-1].
        for (int i = 0; i < size; i++) {
            metrics.steps++;
            metrics.comparisons++;
            if (data[i] == x) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Metrics metrics() {
        return metrics;
    }

    /** Current length of the backing array (used by tests to check the 2x growth). */
    public int capacity() {
        return data.length;
    }

    private void grow() {
        if (data.length > Integer.MAX_VALUE / 2) {
            throw new OutOfMemoryError("DynamicArray is too large");
        }
        int[] bigger = new int[data.length * 2];
        for (int i = 0; i < size; i++) {
            bigger[i] = data[i];
            metrics.moves++;
        }
        data = bigger;
    }
}
