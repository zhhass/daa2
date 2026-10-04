package workload.ds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests shared by DynamicArray and MyLinkedList (java.util is used only here, as the reference). */
abstract class IntListContractTest {

    abstract IntList create();

    private static void assertSame(List<Integer> expected, IntList actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals((int) expected.get(i), actual.get(i), "index " + i);
        }
    }

    @Test
    void emptyStructure() {
        IntList list = create();
        assertEquals(0, list.size());
        assertFalse(list.contains(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
    }

    @Test
    void singleElement() {
        IntList list = create();
        list.add(7);
        assertEquals(1, list.size());
        assertEquals(7, list.get(0));
        assertTrue(list.contains(7));
        assertFalse(list.contains(8));
        assertEquals(7, list.remove(0));
        assertEquals(0, list.size());
        assertFalse(list.contains(7));
    }

    @Test
    void addAtEmptyIndexZeroAndAtEnd() {
        IntList list = create();
        list.add(0, 5);      // into an empty structure
        list.add(1, 6);      // at index == size
        list.add(0, 4);      // at the head
        assertEquals(3, list.size());
        assertEquals(4, list.get(0));
        assertEquals(5, list.get(1));
        assertEquals(6, list.get(2));
    }

    @Test
    void duplicateValues() {
        IntList list = create();
        for (int i = 0; i < 5; i++) {
            list.add(3);
        }
        list.add(2, 9);
        assertEquals(6, list.size());
        assertTrue(list.contains(3));
        assertEquals(3, list.remove(0));
        assertEquals(3, list.remove(0));
        assertEquals(9, list.get(0));
        assertEquals(3, list.get(1));
    }

    @Test
    void firstAndLastIndex() {
        IntList list = create();
        for (int i = 0; i < 10; i++) {
            list.add(i * 10);
        }
        assertEquals(0, list.get(0));
        assertEquals(90, list.get(9));
        assertEquals(90, list.remove(9));
        assertEquals(0, list.remove(0));
        assertEquals(10, list.get(0));
        assertEquals(80, list.get(list.size() - 1));
    }

    @Test
    void invalidIndexThrows() {
        IntList list = create();
        list.add(1);
        list.add(2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, 5));
        assertEquals(2, list.size());              // failed calls change nothing
    }

    @Test
    void containsFindsPresentAndMissingValues() {
        IntList list = create();
        Random rnd = new Random(1);
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            int v = rnd.nextInt(1000) * 2;          // even values only
            list.add(v);
            expected.add(v);
        }
        for (int v : expected) {
            assertTrue(list.contains(v));
        }
        for (int i = 0; i < 100; i++) {
            assertFalse(list.contains(rnd.nextInt(1000) * 2 + 1));   // odd values are never stored
        }
    }

    @Test
    void randomOperationsMatchArrayList() {
        Random rnd = new Random(42);
        IntList list = create();
        List<Integer> expected = new ArrayList<>();
        for (int step = 0; step < 20_000; step++) {
            int op = rnd.nextInt(4);
            if (op == 0 || expected.isEmpty()) {
                int v = rnd.nextInt(100);
                list.add(v);
                expected.add(v);
            } else if (op == 1) {
                int idx = rnd.nextInt(expected.size() + 1);
                int v = rnd.nextInt(100);
                list.add(idx, v);
                expected.add(idx, v);
            } else if (op == 2) {
                int idx = rnd.nextInt(expected.size());
                assertEquals((int) expected.remove(idx), list.remove(idx));
            } else {
                int idx = rnd.nextInt(expected.size());
                assertEquals((int) expected.get(idx), list.get(idx));
            }
            assertEquals(expected.size(), list.size());
        }
        assertSame(expected, list);
    }

    @Test
    void growsPastInitialCapacity() {
        IntList list = create();
        for (int i = 0; i < 10_000; i++) {
            list.add(i);
        }
        assertEquals(10_000, list.size());
        assertEquals(0, list.get(0));
        assertEquals(9_999, list.get(9_999));
        assertEquals(5_000, list.get(5_000));
    }

    @Test
    void countersAreMaintainedInsideOperations() {
        IntList list = create();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        list.metrics().reset();
        list.add(0, -1);                              // head insertion
        assertTrue(list.metrics().moves > 0, "head insertion must report moves");
        list.metrics().reset();
        list.remove(50);
        assertTrue(list.metrics().moves > 0, "middle removal must report moves");
        assertTrue(list.metrics().steps > 0, "middle removal must report steps");
        list.metrics().reset();
        list.contains(-12345);
        assertEquals(list.size(), list.metrics().comparisons);
        assertEquals(list.size(), list.metrics().steps);
    }
}
