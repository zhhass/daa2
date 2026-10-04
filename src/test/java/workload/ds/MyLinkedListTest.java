package workload.ds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MyLinkedListTest extends IntListContractTest {

    @Override
    IntList create() {
        return new MyLinkedList();
    }

    private static MyLinkedList filled(int n) {
        MyLinkedList l = new MyLinkedList();
        for (int i = 0; i < n; i++) {
            l.add(i);
        }
        l.metrics().reset();
        return l;
    }

    @Test
    void getWalksFromTheNearerEnd() {
        MyLinkedList l = filled(1000);
        l.get(0);
        assertEquals(1, l.metrics().steps);              // head: just the start node
        l.metrics().reset();
        l.get(999);
        assertEquals(1, l.metrics().steps);              // tail: just the start node
        l.metrics().reset();
        l.get(100);
        assertEquals(101, l.metrics().steps);            // 100 hops from the head + start node
        l.metrics().reset();
        l.get(900);
        assertEquals(100, l.metrics().steps);            // 99 hops from the tail + start node
    }

    @Test
    void middleAccessCostsAboutHalfTheListFromBothEnds() {
        MyLinkedList l = filled(10_000);
        l.get(5_000);
        assertTrue(l.metrics().steps >= 4_999, "get(n/2) must walk ~n/2 nodes");
    }

    @Test
    void headInsertionUpdatesLinks() {
        MyLinkedList l = filled(10);
        l.add(0, 42);
        assertEquals(3, l.metrics().moves);
        assertEquals(0, l.metrics().steps);              // no traversal needed at the head
    }

    @Test
    void middleInsertionUpdatesFourLinks() {
        MyLinkedList l = filled(10);
        l.add(5, 42);
        assertEquals(4, l.metrics().moves);
        assertTrue(l.metrics().steps > 0);
    }

    @Test
    void removeUnlinksWithTwoUpdates() {
        MyLinkedList l = filled(10);
        l.remove(4);
        assertEquals(2, l.metrics().moves);
    }

    @Test
    void listStaysConsistentWhenEmptiedAndRefilled() {
        MyLinkedList l = new MyLinkedList();
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 5; i++) {
                l.add(0, i);
            }
            for (int i = 0; i < 5; i++) {
                l.remove(l.size() - 1);
            }
            assertEquals(0, l.size());
        }
        l.add(1);
        assertEquals(1, l.get(0));
    }
}
