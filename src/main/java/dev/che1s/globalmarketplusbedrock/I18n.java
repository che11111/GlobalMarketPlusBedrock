package dev.che1s.globalmarketplusbedrock;

/** Shared translation entry for model helpers and visible domain failures. */
public final class I18n {
    private I18n() {}
    public static String text(String key, String... arguments) {
        GlobalMarketPlusBedrock plugin = GlobalMarketPlusBedrock.getInstance();
        return plugin == null || plugin.lang() == null ? key : plugin.lang().text(key, arguments);
    }
}
