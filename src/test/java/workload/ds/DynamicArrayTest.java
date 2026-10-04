package workload.ds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DynamicArrayTest extends IntListContractTest {

    @Override
    IntList create() {
        return new DynamicArray();
    }

    @Test
    void capacityDoublesWhenFull() {
        DynamicArray a = new DynamicArray(4);
        for (int i = 0; i < 4; i++) {
            a.add(i);
        }
        assertEquals(4, a.capacity());
        a.add(4);
        assertEquals(8, a.capacity());
        for (int i = 5; i < 9; i++) {
            a.add(i);
        }
        assertEquals(16, a.capacity());
    }

    @Test
    void invalidCapacityRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DynamicArray(0));
    }

    @Test
    void getCostsOneStep() {
        DynamicArray a = new DynamicArray();
        for (int i = 0; i < 1000; i++) {
            a.add(i);
        }
        a.metrics().reset();
        a.get(777);
        assertEquals(1, a.metrics().steps);
        assertEquals(0, a.metrics().moves);
    }

    @Test
    void insertAndRemoveShiftTheTail() {
        DynamicArray a = new DynamicArray(100);
        for (int i = 0; i < 50; i++) {
            a.add(i);
        }
        a.metrics().reset();
        a.add(10, 99);                                  // shifts elements 10..49 -> 40 moves
        assertEquals(40, a.metrics().moves);
        a.metrics().reset();
        a.remove(10);                                   // shifts the 40 elements after index 10
        assertEquals(40, a.metrics().moves);
        a.metrics().reset();
        a.add(a.size(), 5);                             // append: nothing is shifted
        assertEquals(0, a.metrics().moves);
    }

    @Test
    void resizeCopiesEveryElement() {
        DynamicArray a = new DynamicArray(4);
        for (int i = 0; i < 4; i++) {
            a.add(i);
        }
        a.metrics().reset();
        a.add(4);                                       // triggers the 2x growth: 4 elements copied
        assertEquals(4, a.metrics().moves);
    }
}
