package dev.che1s.globalmarketplusbedrock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

final class RateLimitedLog {
    private static final Map<String, Long> LAST = new ConcurrentHashMap<>();
    private RateLimitedLog() {}

    static void failure(String key, Throwable error) {
        long now = System.currentTimeMillis();
        Long last = LAST.putIfAbsent(key, now);
        if (last != null && (now - last < 60000L || !LAST.replace(key, last, now))) return;
        GlobalMarketPlusBedrock plugin = GlobalMarketPlusBedrock.getInstance();
        Logger logger = plugin == null ? Logger.getLogger("GlobalMarketPlusBedrock") : plugin.getLogger();
        logger.log(Level.WARNING, key, error);
    }
}
