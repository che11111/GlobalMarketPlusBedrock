package dev.che1s.globalmarketplusbedrock.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.che1s.globalmarketplusbedrock.FormManager;
import dev.che1s.globalmarketplusbedrock.GlobalMarketPlusBedrock;

public class PlayerQuitListener
    implements Listener
{
    private final FormManager formManager;

    public PlayerQuitListener(FormManager formManager) {
        this.formManager = formManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        formManager.cleanupPending(player);
    }
}
