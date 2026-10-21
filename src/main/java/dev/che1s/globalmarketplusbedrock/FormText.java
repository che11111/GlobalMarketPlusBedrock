package dev.che1s.globalmarketplusbedrock;

import java.util.Collections;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

final class FormText {
    private final GlobalMarketPlusBedrock plugin;
    FormText(GlobalMarketPlusBedrock plugin) { this.plugin = plugin; }

    String get(String path) { return plugin.lang().text(path); }
    String get(String path, String fallback) { String value = plugin.lang().text(path); return value.equals(path) ? fallback : value; }

    String resolve(Player player, String text, Map<String, String> values) {
        String result = text == null ? "" : text;
        if (result.contains("%") && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = PapiHook.setPlaceholders(player, result);
            } catch (RuntimeException | LinkageError ex) {
                RateLimitedLog.failure(I18n.text("logs.papi-failed"), ex);
            }
        }
        String[] arguments = new String[values.size() * 2];
        int index = 0;
        for (Map.Entry<String, String> value : values.entrySet()) {
            arguments[index++] = value.getKey(); arguments[index++] = value.getValue();
        }
        return dev.che1s.globalmarketplusbedrock.i18n.LanguageManager.format(result, arguments);
    }

    String plain(Player player, String text) {
        return MarketService.stripColors(resolve(player, text, Collections.emptyMap()));
    }

    void message(Player player, String path) {
        String raw = get(path, "").replace("{prefix}", get("messages.prefix", ""));
        if (!raw.isEmpty()) player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            resolve(player, raw, Collections.emptyMap())));
    }
}
