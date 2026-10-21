package dev.che1s.globalmarketplusbedrock;

import dev.che1s.globalmarketplusbedrock.i18n.LanguageManager;
import dev.che1s.globalmarketplusbedrock.metrics.MetricsService;
import dev.che1s.globalmarketplusbedrock.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import dev.che1s.globalmarketplusbedrock.command.MarketFormCommand;
import dev.che1s.globalmarketplusbedrock.listener.PlayerJoinListener;
import dev.che1s.globalmarketplusbedrock.listener.PlayerQuitListener;

public class GlobalMarketPlusBedrock
    extends JavaPlugin
{
    private static GlobalMarketPlusBedrock instance;
    private FormManager formManager;
    private LanguageManager language;
    private UpdateChecker updates;
    private MetricsService metrics;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        language = new LanguageManager(getDataFolder().toPath(), this::getResource, getLogger());
        language.importLegacy(getConfig());
        language.load(getConfig().getString("language", "zh_CN"));

        this.formManager = new FormManager(this);

        MarketFormCommand command = new MarketFormCommand(this, formManager);
        if (getCommand("globalmarketplusbedrock") != null) {
            getCommand("globalmarketplusbedrock").setExecutor(command);
            getCommand("globalmarketplusbedrock").setTabCompleter(command);
        } else {
            getLogger().warning(I18n.text("logs.missing-command"));
        }
        Bukkit.getPluginManager().registerEvents(new GmpCommandBridge(formManager), this);
        Bukkit.getPluginManager().registerEvents(new GmpEvents(formManager), this);

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this, formManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(formManager), this);

        updates = new UpdateChecker(this);
        Bukkit.getPluginManager().registerEvents(updates, this);
        updates.reload();
        metrics = new MetricsService(this);
        metrics.reload();
        logDetectedPlugins();
        getLogger().info(language.text("logs.enabled", "version", getDescription().getVersion(), "language", language.locale()));
    }

    private void logDetectedPlugins() {
        for (String name : new String[] {"GlobalMarketPlus", "floodgate", "PlaceholderAPI", "Geyser-Spigot"}) {
            Plugin dependency = Bukkit.getPluginManager().getPlugin(name);
            if (dependency == null || !dependency.isEnabled()) continue;
            getLogger().info(language.text("logs.plugin-detected", "plugin", dependency.getName(),
                "version", dependency.getDescription().getVersion()));
        }
    }

    @Override
    public void onDisable() {
        if (updates != null) updates.shutdown();
        if (metrics != null) metrics.shutdown();
        if (formManager != null) formManager.shutdown();
        if (language != null) getLogger().info(language.text("logs.disabled"));
        instance = null;
    }

    public LanguageManager lang() { return language; }
    public UpdateChecker updates() { return updates; }

    public boolean reloadSettings() {
        try {
            // Validate before Bukkit reloads; malformed YAML must not silently replace live settings.
            LanguageManager.read(java.nio.file.Files.newBufferedReader(
                getDataFolder().toPath().resolve("config.yml"), java.nio.charset.StandardCharsets.UTF_8));
            reloadConfig();
            formManager.shutdown();
            language.load(getConfig().getString("language", "zh_CN"));
            formManager.reload();
            updates.reload();
            metrics.reload();
            return true;
        } catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException | RuntimeException error) {
            getLogger().log(java.util.logging.Level.WARNING, language.text("logs.reload-failed"), error);
            return false;
        }
    }

    public static GlobalMarketPlusBedrock getInstance() {
        return instance;
    }

    public FormManager getFormManager() {
        return formManager;
    }
}
