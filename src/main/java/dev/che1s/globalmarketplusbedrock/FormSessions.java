package dev.che1s.globalmarketplusbedrock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Consuming an old response must never invalidate the newer screen. */
final class FormSessions<K> {
    private final AtomicLong sequence = new AtomicLong();
    private final Map<K, Long> tokens = new ConcurrentHashMap<>();
    long begin(K player) { long token = sequence.incrementAndGet(); tokens.put(player, token); return token; }
    boolean active(K player, long token) { return Long.valueOf(token).equals(tokens.get(player)); }
    boolean consume(K player, long token) { return tokens.remove(player, token); }
    void forget(K player) { tokens.remove(player); }
    void clear() { tokens.clear(); }
}
