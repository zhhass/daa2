package workload.ds;

import workload.metrics.Metrics;

/**
 * Common interface of DynamicArray and MyLinkedList, so that ONE benchmark
 * can run both structures. Only primitive int values are stored.
 */
public interface IntList {
    /** Appends x to the end. */
    void add(int x);

    /** Inserts x at position index (0 &le; index &le; size). @throws IndexOutOfBoundsException otherwise. */
    void add(int index, int x);

    /** Removes and returns the element at index (0 &le; index &lt; size). @throws IndexOutOfBoundsException otherwise. */
    int remove(int index);

    /** Returns the element at index (0 &le; index &lt; size). @throws IndexOutOfBoundsException otherwise. */
    int get(int index);

    /** Linear search. */
    boolean contains(int x);

    int size();

    /** Counters of physical operations performed so far. */
    Metrics metrics();
}
