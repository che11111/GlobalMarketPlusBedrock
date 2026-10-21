package dev.che1s.globalmarketplusbedrock.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import dev.che1s.globalmarketplusbedrock.BedrockUtil;
import dev.che1s.globalmarketplusbedrock.FormManager;
import dev.che1s.globalmarketplusbedrock.GlobalMarketPlusBedrock;
import dev.che1s.globalmarketplusbedrock.Scheduler;

public class PlayerJoinListener
    implements Listener
{
    private final GlobalMarketPlusBedrock plugin;
    private final FormManager formManager;

    public PlayerJoinListener(GlobalMarketPlusBedrock plugin, FormManager formManager) {
        this.plugin = plugin;
        this.formManager = formManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("auto-open-on-join", false)) return;
        Player player = event.getPlayer();
        long delayTicks = plugin.getConfig().getLong("auto-open-delay", 1L);
        delayTicks = Math.max(1L, Math.min(delayTicks, 3600L)) * 20L;
        Scheduler.runTaskLater(plugin, player, () -> {
            if (plugin.getConfig().getBoolean("auto-open-on-join", false) && player.isOnline() && BedrockUtil.isBedrock(player)) {
                formManager.openMainForm(player);
            }
        }, delayTicks);
    }
}
