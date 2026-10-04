package workload.ds;

import workload.metrics.Metrics;

/**
 * Doubly linked list of primitive ints written from scratch (head and tail pointers).
 * Positional access walks from the nearer end, so index 0 and index size-1 are cheap.
 *
 * Counting rules (done inside the methods):
 *  - steps       : one node visited (the start node counts as 1, every hop to a neighbour adds 1);
 *  - moves       : one link (pointer) update: prev/next of a node, or the head/tail reference;
 *  - comparisons : one comparison of two elements (contains).
 */
public final class MyLinkedList implements IntList {

    private static final class Node {
        final int value;
        Node prev;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
    private int size;
    private final Metrics metrics = new Metrics();

    @Override
    public void add(int x) {
        addLast(x);
    }

    @Override
    public void add(int index, int x) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
        if (index == size) {
            addLast(x);
        } else if (index == 0) {
            addFirst(x);
        } else {
            Node succ = nodeAt(index);
            Node pred = succ.prev;
            Node node = new Node(x);
            node.prev = pred;
            node.next = succ;
            pred.next = node;
            succ.prev = node;
            metrics.moves += 4;
            size++;
        }
    }

    @Override
    public int remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
        Node node = nodeAt(index);
        if (node.prev == null) {
            head = node.next;
        } else {
            node.prev.next = node.next;
        }
        metrics.moves++;
        if (node.next == null) {
            tail = node.prev;
        } else {
            node.next.prev = node.prev;
        }
        metrics.moves++;
        size--;
        return node.value;
    }

    @Override
    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
        return nodeAt(index).value;
    }

    @Override
    public boolean contains(int x) {
        // Loop invariant: before each iteration x does not occur in the nodes before cur.
        Node cur = head;
        while (cur != null) {
            metrics.steps++;
            metrics.comparisons++;
            if (cur.value == x) {
                return true;
            }
            cur = cur.next;
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

    private void addLast(int x) {
        Node node = new Node(x);
        node.prev = tail;
        metrics.moves++;
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        metrics.moves++;
        tail = node;
        metrics.moves++;
        size++;
    }

    private void addFirst(int x) {
        Node node = new Node(x);
        node.next = head;
        metrics.moves++;
        if (head == null) {
            tail = node;
        } else {
            head.prev = node;
        }
        metrics.moves++;
        head = node;
        metrics.moves++;
        size++;
    }

    /** Precondition: 0 <= index < size. Walks from the nearer end. */
    private Node nodeAt(int index) {
        Node cur;
        if (index < size / 2) {
            cur = head;
            metrics.steps++;
            for (int i = 0; i < index; i++) {
                cur = cur.next;
                metrics.steps++;
            }
        } else {
            cur = tail;
            metrics.steps++;
            for (int i = size - 1; i > index; i--) {
                cur = cur.prev;
                metrics.steps++;
            }
        }
        return cur;
    }
}
