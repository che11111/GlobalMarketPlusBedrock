package dev.che1s.globalmarketplusbedrock;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.geysermc.cumulus.util.FormImage;

/** Deliberate formatting prevents custom item names from making product rows bold. */
final class ButtonAppearance {
    private final Map<ButtonKind, FormImage> images = new EnumMap<>(ButtonKind.class);
    private final Map<ButtonKind, String> symbols = new EnumMap<>(ButtonKind.class);
    private final String functionPrefix;
    private final String productPrefix;

    ButtonAppearance(ConfigurationSection config) {
        functionPrefix = color(config.getString("button-style.function-color", "&8"))
            + (config.getBoolean("button-style.function-bold", true) ? "§l" : "");
        productPrefix = color(config.getString("button-style.product-color", "&2"));
        for (ButtonKind kind : ButtonKind.values()) {
            String key = kind.name().toLowerCase(Locale.ROOT);
            String base = "button-style.functions." + key;
            images.put(kind, config.getBoolean("button-style.images", true)
                ? ItemIconResolver.parse(config.getString(base + ".image", "path;;" + kind.path)) : null);
            symbols.put(kind, config.getBoolean("button-style.symbols", true)
                ? MarketService.stripColors(config.getString(base + ".symbol", kind.symbol)) : "");
        }
    }

    FormImage image(ButtonKind kind) { return images.get(kind); }

    String function(ButtonKind kind, String label) {
        String symbol = symbols.get(kind);
        return decorate(label, functionPrefix, symbol.isEmpty() ? "" : symbol + " ");
    }

    String product(String label) { return decorate(label, productPrefix, ""); }

    private static String decorate(String label, String style, String symbol) {
        // Repeat formatting on each line and reset at the end to avoid style bleed.
        String plain = MarketService.stripColors(label);
        return "§r" + style + symbol + plain.replace("\n", "§r\n" + style) + "§r";
    }

    private static String color(String raw) {
        // Accept only color codes here, never bold/obfuscated formatting for products.
        String translated = ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i + 1 < translated.length(); i++) {
            if (translated.charAt(i) == '§' && "0123456789abcdef".indexOf(translated.charAt(i + 1)) >= 0) {
                result.append('§').append(translated.charAt(++i));
            }
        }
        return result.toString();
    }
}
