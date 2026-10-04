package workload.metrics;

/**
 * Operation counters. Every data structure owns one instance and increments it
 * INSIDE its methods (never estimated afterwards).
 *
 * <ul>
 *   <li><b>steps</b> - one read of an array cell, or one node visited while walking a list;</li>
 *   <li><b>moves</b> - one element shifted/copied inside an array, or one link (pointer) update in a list;</li>
 *   <li><b>comparisons</b> - one comparison of two elements.</li>
 * </ul>
 */
public final class Metrics {
    public long steps;
    public long moves;
    public long comparisons;

    public void reset() {
        steps = 0;
        moves = 0;
        comparisons = 0;
    }

    @Override
    public String toString() {
        return "steps=" + steps + ", moves=" + moves + ", comparisons=" + comparisons;
    }
}
