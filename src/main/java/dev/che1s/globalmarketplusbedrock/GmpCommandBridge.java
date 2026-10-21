package dev.che1s.globalmarketplusbedrock;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import studio.trc.bukkit.globalmarketplus.api.APIUtils;

final class GmpCommandBridge implements Listener {
    private final FormManager manager;
    GmpCommandBridge(FormManager manager) { this.manager = manager; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void command(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!manager.ui.plugin.getConfig().getBoolean("bridge-gmp-commands", true)
                || !player.hasPermission("globalmarketplusbedrock.use") || !BedrockUtil.isBedrock(player)) return;
        try {
            String command = event.getMessage().substring(1);
            String root = command.split("\\s+", 2)[0];
            Command registered = Bukkit.getPluginCommand(root);
            if (!(registered instanceof PluginIdentifiableCommand)
                    || !((PluginIdentifiableCommand) registered).getPlugin().getName().equals("GlobalMarketPlus")) return;
            String suffix = command.substring(root.length());
            String plainRoot = root.contains(":") ? root.substring(root.indexOf(':') + 1) : root;
            if (plainRoot.equalsIgnoreCase("gmp") || plainRoot.equalsIgnoreCase("globalmarket") || plainRoot.equalsIgnoreCase("globalmarketplus")) {
                command = "gmp" + suffix;
            } else {
                YamlConfiguration aliases = APIUtils.getConfig("Alias.yml");
                String mapped = aliases == null ? null : aliases.getString("Aliases." + plainRoot);
                if (mapped == null) return;
                command = mapped + suffix;
            }
            GmpRoute route = GmpRoute.parse(command);
            if (route == null) return;
            if ((route.kind == GmpRoute.Kind.PROFILE || route.kind == GmpRoute.Kind.CATEGORIES)
                    && route.target != null && !route.target.equalsIgnoreCase(player.getName())) return;
            event.setCancelled(true);
            Scheduler.runTask(player, () -> manager.ui.safe(player, () -> manager.route(player, route)));
        } catch (RuntimeException | LinkageError ex) {
            RateLimitedLog.failure(I18n.text("logs.bridge-failed"), ex);
            // Leave the original command intact when it has not been claimed.
        }
    }
}
