package dev.che1s.globalmarketplusbedrock;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.cumulus.util.FormImage;

/** Resolves button icons once at startup; never performs network I/O. */
final class ItemIconResolver {
    static final String TEXTURE_VERSION = "26.3";
    static final String DEFAULT_URL = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{version}/assets/minecraft/textures/{path}.png";
    private final Map<String, FormImage> materials = new HashMap<>();
    private final Map<String, FormImage> models = new HashMap<>();
    private final boolean enabled;
    private final FormImage fallback;

    ItemIconResolver(GlobalMarketPlusBedrock plugin) {
        this(plugin.getConfig(), () -> plugin.getResource("item-textures.yml"), plugin.getLogger()::warning);
    }

    ItemIconResolver(ConfigurationSection config, Supplier<InputStream> mappingResource, Consumer<String> warning) {
        enabled = config.getBoolean("item-icons.enabled", true);
        fallback = parse(config.getString("item-icons.fallback", ""));
        if (!enabled) return;

        if (config.getBoolean("item-icons.auto.enabled", true)) {
            String version = config.getString("item-icons.auto.version", TEXTURE_VERSION);
            String format = config.getString("item-icons.auto.url-format", DEFAULT_URL);
            // Old installations otherwise request newly introduced textures from a 1.20.4 archive (404).
            if (("1.20.4".equals(version) || "1.21.11".equals(version)) && DEFAULT_URL.equals(format)
                    && config.getBoolean("item-icons.auto.upgrade-legacy-default", true)) version = TEXTURE_VERSION;
            try (InputStream input = mappingResource.get()) {
                if (input == null) throw new IllegalStateException("Missing item-textures.yml");
                YamlConfiguration mapping = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
                for (String material : mapping.getKeys(false)) {
                    materials.put(material, parse("url;;" + format.replace("{version}", version)
                        .replace("{path}", mapping.getString(material, ""))
                        .replace("{material}", material.toLowerCase(Locale.ROOT))));
                }
            } catch (Exception ex) {
                warning.accept(I18n.text("logs.icon-map-failed") + ex);
            }
        }

        ConfigurationSection overrides = config.getConfigurationSection("item-icons.materials");
        if (overrides != null) {
            for (String material : overrides.getKeys(false)) {
                String raw = overrides.getString(material, "");
                materials.put(material.toUpperCase(Locale.ROOT), checked(warning, material, raw));
            }
        }
        ConfigurationSection custom = config.getConfigurationSection("item-icons.custom-model-data");
        if (custom != null) {
            for (String material : custom.getKeys(false)) {
                ConfigurationSection values = custom.getConfigurationSection(material);
                if (values == null) continue;
                for (String model : values.getKeys(false)) {
                    String key = material.toUpperCase(Locale.ROOT) + ":" + model;
                    models.put(key, checked(warning, key, values.getString(model, "")));
                }
            }
        }
    }

    FormImage resolve(ItemStack item) {
        if (!enabled || item == null || item.getType().isAir()) return null;
        Integer model = null;
        if (!models.isEmpty() && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasCustomModelData()) model = meta.getCustomModelData();
        }
        return resolve(item.getType().name(), model);
    }

    FormImage resolve(String material, Integer model) {
        if (!enabled) return null;
        String key = material + ":" + model;
        if (model != null && models.containsKey(key)) return models.get(key);
        return materials.containsKey(material) ? materials.get(material) : fallback;
    }

    private static FormImage checked(Consumer<String> warning, String key, String raw) {
        FormImage image = parse(raw);
        if (image == null && raw != null && !raw.trim().isEmpty() && !raw.equalsIgnoreCase("none")) {
            warning.accept(I18n.text("logs.icon-invalid-prefix") + key + I18n.text("logs.icon-invalid-suffix"));
        }
        return image;
    }

    static FormImage parse(String raw) {
        if (raw == null) return null;
        String[] parts = raw.trim().split(";;", 2);
        if (parts.length != 2 || parts[1].trim().isEmpty()) return null;
        String value = parts[1].trim();
        if (parts[0].equalsIgnoreCase("path")) {
            return FormImage.of(FormImage.Type.PATH, value);
        }
        if (parts[0].equalsIgnoreCase("url")) {
            try {
                URI uri = URI.create(value);
                if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                        && uri.getHost() != null) {
                    return FormImage.of(FormImage.Type.URL, value);
                }
            } catch (IllegalArgumentException ignored) {
                // An invalid icon must not prevent the product button from being added.
            }
        }
        return null;
    }
}
