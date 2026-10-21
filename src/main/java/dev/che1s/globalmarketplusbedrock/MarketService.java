package dev.che1s.globalmarketplusbedrock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import studio.trc.bukkit.globalmarketplus.api.GlobalMarket;
import studio.trc.bukkit.globalmarketplus.api.ItemDisplay;
import studio.trc.bukkit.globalmarketplus.api.Mailbox;
import studio.trc.bukkit.globalmarketplus.api.Merchant;
import studio.trc.bukkit.globalmarketplus.mailbox.ItemMail;
import studio.trc.bukkit.globalmarketplus.mailbox.ItemMailType;
import studio.trc.bukkit.globalmarketplus.merchandise.Merchandise;
import studio.trc.bukkit.globalmarketplus.merchandise.MerchandiseType;
import studio.trc.bukkit.globalmarketplus.merchandise.auction.Auction;

/** Read through public APIs. Writes always use GMP commands with GMP validation. */
public final class MarketService {
    private MarketService() {}

    public static Merchant getMerchant(Player player) { return require(Merchant.getMerchant(player), I18n.text("errors.merchant-not-loaded")); }
    static Merchant merchant(UUID owner) { return require(Merchant.getMerchant(owner), I18n.text("errors.merchant-missing")); }
    static Merchant merchant(String name) { return require(Merchant.getMerchant(name), I18n.text("errors.merchant-not-found")); }
    static <T> T require(T value, String message) {
        if (value == null) throw new MarketException(message);
        return value;
    }
    static List<Merchandise> all() {
        List<Merchandise> list = require(GlobalMarket.getGlobalMarket(), I18n.text("errors.market-unavailable")).getMerchandises();
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }
    static Merchandise live(long uid) {
        Merchandise item = require(GlobalMarket.getGlobalMarket().getMerchandise(uid), I18n.text("errors.item-removed"));
        if (item.isExpired()) throw new MarketException(I18n.text("errors.item-expired"));
        return item;
    }
    static List<ItemMail> mails(UUID owner) {
        List<ItemMail> list = require(Mailbox.getMailbox(owner), I18n.text("errors.mail-not-loaded")).getItemMails();
        return list == null ? Collections.emptyList() : new ArrayList<>(list);
    }
    static ItemMail mail(UUID owner, long uid) {
        return require(Mailbox.getMailbox(owner).getItemMail(uid), I18n.text("errors.mail-removed"));
    }
    public static List<Merchandise> getPlayerMerchandises(Player player) {
        List<Merchandise> result = new ArrayList<>();
        List<Merchandise> list = getMerchant(player).getMerchandises();
        if (list != null) for (Merchandise m : list) if (m != null && !m.isExpired()) result.add(m);
        return result;
    }
    public static List<ItemMail> getMails(Player player) {
        List<ItemMail> result = new ArrayList<>();
        for (ItemMail m : mails(player.getUniqueId())) if (m != null && !m.isPickedUp() && !m.isExpired()) result.add(m);
        return result;
    }
    public static String getBalanceDisplay(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                String value = PapiHook.setPlaceholders(player, "%gmpapi_merchant_balance_display%");
                if (value != null && !value.contains("%gmpapi_")) return value;
            } catch (RuntimeException | LinkageError ex) {
                RateLimitedLog.failure(I18n.text("logs.balance-failed"), ex);
            }
        }
        return formatMoney(getMerchant(player).getDefaultBalance());
    }
    static boolean publicListing(Merchandise item, Merchant owner) {
        return !item.isExpired() && !item.getMerchandiseOption().isHiddenMerchandise()
            && !owner.getProfile().isHiddenMerchandises();
    }
    public static String getSellerDisplay(Merchandise item) {
        return getSellerDisplay(item, merchant(item.getOwnerUUID()));
    }
    static String getSellerDisplay(Merchandise item, Merchant owner) {
        return item.getMerchandiseOption().isHiddenMerchantName() || owner.getProfile().isHiddenMerchantName()
            ? I18n.text("common.anonymous") : stripColors(item.getOwnerName());
    }
    static String currency(Merchandise item) {
        return item.getCurrency() == null ? "Default" : item.getCurrency().getName();
    }
    static boolean command(Player player, String command) {
        if (command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0 || command.length() > 2048) {
            throw new IllegalArgumentException("Invalid command input");
        }
        // A true result means dispatched, not a successful transaction.
        return player.performCommand(command);
    }

    public static String getItemName(ItemStack item) {
        if (item == null) {
            return I18n.text("common.unknown-item");
        }
        try {
            String name = ItemDisplay.getInstance().getItemDisplayName(item);
            if (name != null && !name.isEmpty()) {
                return stripColors(name);
            }
        } catch (RuntimeException | LinkageError t) {
            RateLimitedLog.failure(I18n.text("logs.item-name-failed"), t); // fall through
        }
        try {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return stripColors(item.getItemMeta().getDisplayName());
            }
        } catch (RuntimeException | LinkageError t) {
            RateLimitedLog.failure(I18n.text("logs.item-meta-failed"), t);
        }
        String type = item.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return type;
    }

    public static String stripColors(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }

    public static String formatMoney(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Money must be finite");
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static boolean isNeverExpire(Merchandise merchandise) {
        try {
            return merchandise.getMerchandiseOption() != null
                && merchandise.getMerchandiseOption().isNeverExpire();
        } catch (RuntimeException | LinkageError t) {
            RateLimitedLog.failure(I18n.text("logs.item-options-failed"), t);
            return false;
        }
    }

    public static boolean isAllowRetail(Merchandise merchandise) {
        try {
            return merchandise.isAllowRetail();
        } catch (RuntimeException | LinkageError t) {
            RateLimitedLog.failure(I18n.text("logs.item-options-failed"), t);
            return false;
        }
    }

    public static boolean isAllowWholesale(Merchandise merchandise) {
        try {
            return merchandise.isAllowWholesale();
        } catch (RuntimeException | LinkageError t) {
            RateLimitedLog.failure(I18n.text("logs.item-options-failed"), t);
            return false;
        }
    }

    public static String formatRemaining(long expireTime, boolean neverExpire) {
        if (neverExpire) {
            return I18n.text("common.permanent");
        }
        long remaining = expireTime - System.currentTimeMillis();
        if (remaining <= 0) {
            return I18n.text("common.expired");
        }
        long days = remaining / 86400000L;
        long hours = (remaining % 86400000L) / 3600000L;
        long minutes = (remaining % 3600000L) / 60000L;
        if (days > 0) {
            return days + I18n.text("time.days") + hours + I18n.text("time.hours");
        }
        if (hours > 0) {
            return hours + I18n.text("time.hours") + minutes + I18n.text("time.minutes");
        }
        return minutes + I18n.text("time.minutes");
    }

    public static String getTypeLabel(Merchandise merchandise) {
        MerchandiseType type = merchandise.getMerchandiseType();
        if (type == MerchandiseType.SELLING) {
            return I18n.text("market.selling");
        }
        if (type == MerchandiseType.PURCHASING) {
            return I18n.text("market.purchasing");
        }
        if (type == MerchandiseType.BIDDING) {
            return I18n.text("market.bidding");
        }
        return String.valueOf(type);
    }

    public static Auction asAuction(Merchandise merchandise) {
        if (merchandise instanceof Auction) {
            return (Auction) merchandise;
        }
        return null;
    }

    public static String getMailSource(ItemMail mail) {
        try {
            ItemMailType type = mail.getMailType();
            if (type == ItemMailType.FROM_TRANSACTION) {
                return I18n.text("mail.source-transaction");
            }
            if (type == ItemMailType.FROM_AUCTION) {
                return I18n.text("market.bidding");
            }
            if (type == ItemMailType.FROM_GIFT) {
                return I18n.text("mail.source-gift");
            }
            if (type == ItemMailType.FROM_EXPIRE) {
                return I18n.text("mail.source-expire");
            }
            if (type == ItemMailType.FROM_REMOVE) {
                return I18n.text("mail.source-remove");
            }
            if (type == ItemMailType.FROM_FORCED_REMOVE) {
                return I18n.text("mail.source-forced-remove");
            }
            if (type == ItemMailType.FROM_RETURNED) {
                return I18n.text("mail.source-returned");
            }
            if (type == ItemMailType.FROM_MIGRATE) {
                return I18n.text("mail.source-migrate");
            }
            return I18n.text("mail.source-other");
        } catch (RuntimeException | LinkageError t) {
            RateLimitedLog.failure(I18n.text("logs.mail-source-failed"), t);
            return I18n.text("mail.source-other");
        }
    }

}
