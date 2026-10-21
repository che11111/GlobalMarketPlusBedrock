package dev.che1s.globalmarketplusbedrock;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Cache only display data; failed loads never replace a valid entry. */
final class SnapshotCache<T> {
    private final long ttl;
    private final LongSupplier clock;
    private T value;
    private long until;
    SnapshotCache(long ttl, LongSupplier clock) { this.ttl = ttl; this.clock = clock; }
    synchronized T get(Supplier<T> load) {
        long now = clock.getAsLong();
        if (value == null || now >= until) {
            T next = load.get();
            value = next;
            until = clock.getAsLong() + ttl;
        }
        return value;
    }
    synchronized void invalidate() { value = null; until = 0; }
}
