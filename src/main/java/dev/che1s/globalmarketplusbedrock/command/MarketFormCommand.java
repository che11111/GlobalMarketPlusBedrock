package dev.che1s.globalmarketplusbedrock.command;

import dev.che1s.globalmarketplusbedrock.I18n;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import dev.che1s.globalmarketplusbedrock.FormManager;
import dev.che1s.globalmarketplusbedrock.GlobalMarketPlusBedrock;

public class MarketFormCommand
    implements CommandExecutor, TabCompleter
{
    private final GlobalMarketPlusBedrock plugin;
    private final FormManager formManager;

    public MarketFormCommand(GlobalMarketPlusBedrock plugin, FormManager formManager) {
        this.plugin = plugin;
        this.formManager = formManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission("globalmarketplusbedrock.admin")) {
                sender.sendMessage(plugin.lang().message("messages.no-permission")); return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(plugin.lang().message(plugin.reloadSettings() ? "messages.reload-success" : "messages.reload-failed"));
            } else if (args.length == 1 && args[0].equalsIgnoreCase("update")) plugin.updates().request(sender);
            else sender.sendMessage(plugin.lang().message("messages.admin-help"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.lang().message("messages.player-only")); return true;
        }
        Player player = (Player) sender;
        formManager.openMainForm(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender.hasPermission("globalmarketplusbedrock.admin") && args.length == 1) {
            java.util.List<String> matches = new java.util.ArrayList<>();
            for (String value : new String[] {"reload", "update"}) if (value.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) matches.add(value);
            return matches;
        }
        return Collections.emptyList();
    }
}
