package dev.che1s.globalmarketplusbedrock;

import me.clip.placeholderapi.PlaceholderAPI;

import org.bukkit.entity.Player;

/** Isolated access to the mandatory PlaceholderAPI dependency. */
public final class PapiHook
{
    private PapiHook() {}

    public static String setPlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
