package dev.che1s.globalmarketplusbedrock;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import studio.trc.bukkit.globalmarketplus.api.APIUtils;
import studio.trc.bukkit.globalmarketplus.api.Merchant;

/** Read guards follow GMP's configurable Default/Permission/Allowed-Groups rules. */
final class GmpAccess {
    private GmpAccess() {}

    static boolean command(Player player, String path) {
        String[] segments = path.split("\\.");
        String current = "";
        for (String segment : segments) {
            current = current.isEmpty() ? segment : current + "." + segment;
            if (!rule(player, "CommandPermissions.yml", current)) return false;
        }
        return true;
    }

    static void require(Player player, String path) {
        String root = path.split("\\.")[0];
        String module = root.equals("Mailbox") ? "Mailbox" : root.equals("Profile") ? "Profile" : "GlobalMarket";
        YamlConfiguration settings = MarketService.require(APIUtils.getConfig("Config.yml"), I18n.text("errors.gmp-config"));
        boolean market = settings.getBoolean("Module-Switches.GlobalMarket", true);
        if (!(market && (module.equals("Mailbox") || module.equals("Profile")))
                && !settings.getBoolean("Module-Switches." + module, true)) throw new MarketException(I18n.text("errors.module-disabled"));
        if (!command(player, path)) throw new MarketException(I18n.text("errors.gmp-permission"));
    }

    static void environment(Player player) {
        YamlConfiguration settings = MarketService.require(APIUtils.getConfig("Config.yml"), I18n.text("errors.gmp-config"));
        if (settings.getStringList("Disabled-Worlds").contains(player.getWorld().getName())
                && !rule(player, "OtherPermissions.yml", "Disabled-Worlds-Bypass")) {
            throw new MarketException(I18n.text("errors.world-disabled"));
        }
    }

    static boolean rule(Player player, String file, String path) {
        return rule(player, MarketService.require(APIUtils.getConfig(file), I18n.text("errors.gmp-permission-config") + file), path);
    }

    static boolean rule(Player player, YamlConfiguration config, String path) {
        if (!config.isConfigurationSection(path)) return false;
        if (config.getBoolean(path + ".Default", false)) return true;
        String permission = config.getString(path + ".Permission", "");
        if (!permission.isEmpty() && player.hasPermission(permission)) return true;
        if (config.getStringList(path + ".Allowed-Groups").isEmpty()) return false;
        Merchant merchant = MarketService.getMerchant(player);
        return config.getStringList(path + ".Allowed-Groups").contains(merchant.getGroup().getGroupName());
    }
}
