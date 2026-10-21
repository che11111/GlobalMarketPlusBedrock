package dev.che1s.globalmarketplusbedrock;

/** A visible service failure, never interpreted as an empty market. */
final class MarketException extends RuntimeException {
    MarketException(String message) { super(message); }
    MarketException(String message, Throwable cause) { super(message, cause); }
}
