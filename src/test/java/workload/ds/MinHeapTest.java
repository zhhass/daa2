package workload.ds;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MinHeapTest {

    @Test
    void emptyHeapThrowsIllegalState() {
        MinHeap h = new MinHeap();
        assertTrue(h.isEmpty());
        assertThrows(IllegalStateException.class, h::peekMin);
        assertThrows(IllegalStateException.class, h::extractMin);
    }

    @Test
    void extractFromHeapThatBecameEmptyThrows() {
        MinHeap h = new MinHeap();
        h.insert(1);
        assertEquals(1, h.extractMin());
        assertThrows(IllegalStateException.class, h::extractMin);
        assertThrows(IllegalStateException.class, h::peekMin);
    }

    @Test
    void singleElement() {
        MinHeap h = new MinHeap();
        h.insert(5);
        assertEquals(5, h.peekMin());
        assertEquals(1, h.size());
        assertEquals(5, h.extractMin());
        assertEquals(0, h.size());
    }

    @Test
    void duplicateValues() {
        MinHeap h = new MinHeap();
        for (int i = 0; i < 20; i++) {
            h.insert(i % 3);
            assertTrue(h.isHeap());
        }
        int prev = Integer.MIN_VALUE;
        for (int i = 0; i < 20; i++) {
            int v = h.extractMin();
            assertTrue(prev <= v);
            prev = v;
        }
    }

    @Test
    void extremeValues() {
        MinHeap h = new MinHeap();
        h.insert(Integer.MAX_VALUE);
        h.insert(Integer.MIN_VALUE);
        h.insert(0);
        assertEquals(Integer.MIN_VALUE, h.extractMin());
        assertEquals(0, h.extractMin());
        assertEquals(Integer.MAX_VALUE, h.extractMin());
    }

    @Test
    void heapPropertyHoldsAfterEveryInsertAndExtract() {
        Random rnd = new Random(42);
        MinHeap h = new MinHeap();
        for (int i = 0; i < 2000; i++) {
            h.insert(rnd.nextInt(500));
            assertTrue(h.isHeap(), "after insert #" + i);
        }
        while (!h.isEmpty()) {
            h.extractMin();
            assertTrue(h.isHeap(), "after extract, size=" + h.size());
        }
    }

    @Test
    void nExtractionsReturnNonDecreasingValues() {
        Random rnd = new Random(42);
        int n = 20_000;
        int[] data = new int[n];
        MinHeap h = new MinHeap();
        for (int i = 0; i < n; i++) {
            data[i] = rnd.nextInt();
            h.insert(data[i]);
        }
        Arrays.sort(data);
        for (int i = 0; i < n; i++) {
            assertEquals(data[i], h.extractMin(), "position " + i);
        }
        assertTrue(h.isEmpty());
    }

    @Test
    void interleavedOperationsMatchPriorityQueue() {
        Random rnd = new Random(7);
        MinHeap h = new MinHeap();
        PriorityQueue<Integer> ref = new PriorityQueue<>();
        for (int step = 0; step < 30_000; step++) {
            if (ref.isEmpty() || rnd.nextInt(3) != 0) {
                int v = rnd.nextInt(1000) - 500;
                h.insert(v);
                ref.add(v);
            } else {
                assertEquals((int) ref.poll(), h.extractMin());
            }
            assertEquals(ref.size(), h.size());
            if (!ref.isEmpty()) {
                assertEquals((int) ref.peek(), h.peekMin());
            }
        }
        assertTrue(h.isHeap());
    }

    @Test
    void peekMinCostsOneStepAndDoesNotChangeTheHeap() {
        MinHeap h = new MinHeap();
        for (int i = 100; i > 0; i--) {
            h.insert(i);
        }
        h.metrics().reset();
        assertEquals(1, h.peekMin());
        assertEquals(1, h.metrics().steps);
        assertEquals(0, h.metrics().moves);
        assertEquals(0, h.metrics().comparisons);
        assertEquals(100, h.size());
    }

    @Test
    void sortedInsertionNeedsOneComparisonPerElement() {
        MinHeap h = new MinHeap(100);
        for (int i = 0; i < 100; i++) {
            h.insert(i);                                // ascending input: bubble-up stops at once
        }
        assertEquals(99, h.metrics().comparisons);      // the very first insert has no parent
        assertEquals(0, h.metrics().moves);
    }

    @Test
    void countersGrowInsideOperations() {
        MinHeap h = new MinHeap(1000);
        Random rnd = new Random(1);
        for (int i = 0; i < 1000; i++) {
            h.insert(rnd.nextInt());
        }
        long cmpAfterInserts = h.metrics().comparisons;
        assertTrue(cmpAfterInserts > 0);
        h.extractMin();
        assertTrue(h.metrics().comparisons > cmpAfterInserts);
    }

    @Test
    void buildHeapProducesValidHeapOnRandomData() {
        Random rnd = new Random(3);
        for (int n : new int[] {0, 1, 2, 3, 10, 1000, 4097}) {
            int[] data = new int[n];
            for (int i = 0; i < n; i++) {
                data[i] = rnd.nextInt(100);
            }
            int[] copy = data.clone();
            MinHeap h = MinHeap.buildHeap(data);
            assertArrayEquals(copy, data, "input must not be modified");
            assertEquals(n, h.size());
            assertTrue(h.isHeap(), "n=" + n);
            Arrays.sort(copy);
            for (int i = 0; i < n; i++) {
                assertEquals(copy[i], h.extractMin());
            }
        }
    }

    @Test
    void buildHeapUsesFewerComparisonsThanRepeatedInsert() {
        Random rnd = new Random(42);
        int n = 50_000;
        int[] data = new int[n];
        for (int i = 0; i < n; i++) {
            data[i] = rnd.nextInt();
        }
        MinHeap built = MinHeap.buildHeap(data);
        MinHeap inserted = new MinHeap(n);
        for (int v : data) {
            inserted.insert(v);
        }
        assertTrue(built.metrics().comparisons <= 2L * n, "Floyd needs at most 2n comparisons");
        assertTrue(built.metrics().comparisons < inserted.metrics().comparisons);
        assertFalse(built.isEmpty());
    }
}
