package dev.che1s.globalmarketplusbedrock;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import studio.trc.bukkit.globalmarketplus.api.APIUtils;

/** Single-item listing owns its countdown; native bulk listings own their inventory. */
final class ListingForms {
    enum Kind { SELL, PURCHASE, AUCTION }
    private final FormManager manager;
    private final FormUi ui;
    private final Map<UUID, BossBar> countdowns = new ConcurrentHashMap<>();
    ListingForms(FormManager manager) { this.manager = manager; ui = manager.ui; }

    void open(Player player) {
        ui.screen(player, ui.text.get("form.listing.title"), ui.text.get("form.listing.content"))
            .button(ButtonKind.MARKET, ui.text.get("form.listing.button-sell"), () -> start(player, Kind.SELL))
            .button(ButtonKind.PURCHASE, ui.text.get("form.listing.button-purchase"), () -> start(player, Kind.PURCHASE))
            .button(ButtonKind.AUCTION, ui.text.get("form.listing.button-auction"), () -> start(player, Kind.AUCTION))
            .button(ButtonKind.LISTING, I18n.text("listing.bulk-sell-native"), () -> bulk(player, false))
            .button(ButtonKind.LISTING, I18n.text("listing.bulk-buy-native"), () -> bulk(player, true))
            .button(ButtonKind.BACK, I18n.text("common.back"), () -> manager.openMainForm(player)).send();
    }

    private void start(Player player, Kind kind) {
        GmpAccess.require(player, kind == Kind.SELL ? "Market.Sell" : kind == Kind.PURCHASE ? "Market.Buy" : "Auction.Start");
        cleanup(player);
        int seconds = Math.max(0, Math.min(300, ui.plugin.getConfig().getInt("countdown.seconds", 10)));
        if (seconds == 0) { price(player, kind); return; }
        BossBar bar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', ui.text.get("countdown.bossbar")), BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(player);
        countdowns.put(player.getUniqueId(), bar);
        tick(player, kind, bar, seconds, seconds);
    }

    private void tick(Player player, Kind kind, BossBar bar, int left, int total) {
        if (countdowns.get(player.getUniqueId()) != bar) return;
        if (!player.isOnline() || !player.hasPermission("globalmarketplusbedrock.use")) { cleanup(player); return; }
        if (left == 0) {
            cleanup(player);
            ui.safe(player, () -> price(player, kind));
            return;
        }
        bar.setProgress(left / (double) total);
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                ui.text.get("countdown.actionbar").replace("{second}", String.valueOf(left)))));
        Scheduler.runTaskLater(ui.plugin, player, () -> tick(player, kind, bar, left - 1, total), 20L);
    }

    private void price(Player player, Kind kind) {
        ItemStack expected = player.getInventory().getItemInMainHand().clone();
        int slot = player.getInventory().getHeldItemSlot();
        if (expected.getType().isAir() || expected.getAmount() <= 0) {
            ui.text.message(player, "messages.listing-no-item"); open(player); return;
        }
        CustomForm.Builder form = CustomForm.builder().title(kind == Kind.SELL ? I18n.text("listing.sell-title") : kind == Kind.PURCHASE ? I18n.text("listing.purchase-title") : I18n.text("listing.auction-title"))
            .label(I18n.text("labels.item") + MarketService.getItemName(expected) + " x" + expected.getAmount()
                + I18n.text("listing.rules"))
            .input(kind == Kind.AUCTION ? I18n.text("listing.start-price") : I18n.text("listing.price-label"), "0", "")
            .input(I18n.text("listing.amount"), "", String.valueOf(expected.getAmount()));
        if (kind != Kind.AUCTION) form.dropdown(I18n.text("listing.mode"), Arrays.asList(I18n.text("listing.wholesale"), I18n.text("listing.retail")), 0);
        ui.custom(player, form, response -> {
            try {
                // Component indexes include the label, so explicitly skip labels for sequential input reads.
                response.includeLabels(false);
                double value = NumberInput.decimal(response.asInput());
                int amount = NumberInput.amount(response.asInput());
                boolean retail = kind != Kind.AUCTION && response.asDropdown() == 1;
                if (value < 0 || amount <= 0 || (kind != Kind.PURCHASE && amount > expected.getAmount())) throw new IllegalArgumentException("Invalid listing");
                if (player.getInventory().getHeldItemSlot() != slot || !expected.equals(player.getInventory().getItemInMainHand())) {
                    ui.text.message(player, "messages.listing-item-changed"); open(player); return;
                }
                String command = kind == Kind.AUCTION ? "gmp auction start " + MarketService.formatMoney(value)
                    : "gmp market " + (kind == Kind.SELL ? "sell " : "buy ") + mode(retail) + ":" + MarketService.formatMoney(value);
                ui.confirm(player, I18n.text("listing.confirm-title"), I18n.text("labels.product") + MarketService.getItemName(expected) + I18n.text("labels.amount") + amount
                    + "\n" + (kind == Kind.AUCTION ? I18n.text("labels.start-price") : retail ? I18n.text("labels.unit-price") : I18n.text("labels.lot-price")) + MarketService.formatMoney(value), () -> {
                        if (player.getInventory().getHeldItemSlot() != slot || !expected.equals(player.getInventory().getItemInMainHand())) {
                            ui.text.message(player, "messages.listing-item-changed"); open(player); return;
                        }
                        manager.actions.submit(player, command + " " + amount, () -> manager.openMyMerchandisesForm(player, 0));
                    }, () -> open(player));
            } catch (IllegalArgumentException ex) {
                ui.text.message(player, "messages.invalid-number"); price(player, kind);
            }
        }, () -> open(player));
    }

    private String mode(boolean retail) {
        String name = MarketService.require(APIUtils.getConfig("GlobalMarket.yml"), I18n.text("errors.market-config"))
            .getString("Marketing-Mode-Name." + (retail ? "Retail" : "Wholesale"));
        if (name == null || name.isEmpty() || name.contains(":") || name.matches(".*\\s.*")) throw new MarketException(I18n.text("errors.mode-name"));
        return name;
    }

    void bulk(Player player, boolean purchase) {
        GmpAccess.require(player, purchase ? "Market.BuyInventory" : "Market.SellInventory");
        ui.input(player, purchase ? I18n.text("listing.bulk-buy") : I18n.text("listing.bulk-sell"), I18n.text("listing.bulk-price-label"), "", raw -> {
            double value = NumberInput.decimal(raw);
            if (value < 0) throw new IllegalArgumentException("Negative price");
            ui.confirm(player, I18n.text("listing.bulk-open-title"), I18n.text("listing.bulk-confirm"), () -> {
                ui.forget(player);
                manager.invalidate();
                if (!MarketService.command(player, "gmp market " + (purchase ? "buyinventory " : "sellinventory ") + MarketService.formatMoney(value))) {
                    throw new MarketException(I18n.text("errors.bulk-open"));
                }
            }, () -> open(player));
        }, () -> open(player));
    }

    void cleanup(Player player) {
        BossBar bar = countdowns.remove(player.getUniqueId());
        if (bar != null) bar.removeAll();
    }
    void shutdown() { for (BossBar bar : countdowns.values()) bar.removeAll(); countdowns.clear(); }
}
