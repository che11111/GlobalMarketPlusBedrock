package dev.che1s.globalmarketplusbedrock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import studio.trc.bukkit.globalmarketplus.api.Merchant;
import studio.trc.bukkit.globalmarketplus.mailbox.ItemMail;
import studio.trc.bukkit.globalmarketplus.merchandise.Merchandise;
import studio.trc.bukkit.globalmarketplus.merchandise.MerchandiseType;

/** Routes menus; rendering, workflows, queries and actions live in focused services. */
public final class FormManager {
    final FormUi ui;
    volatile MarketCatalog catalog;
    volatile MarketActions actions;
    final MarketForms market;
    final MailForms mail;
    final ProfileForms profiles;
    final ListingForms listings;
    final ContainerForms containers;

    public FormManager(GlobalMarketPlusBedrock plugin) {
        ui = new FormUi(plugin);
        catalog = new MarketCatalog(plugin);
        actions = new MarketActions(ui, catalog);
        market = new MarketForms(this);
        mail = new MailForms(this);
        profiles = new ProfileForms(this);
        listings = new ListingForms(this);
        containers = new ContainerForms(ui);
        ui.onOpen = listings::cleanup;
    }

    public void openMainForm(Player player) {
        ui.safe(player, () -> {
            Merchant merchant = MarketService.getMerchant(player);
            Map<String, String> values = new HashMap<>();
            values.put("balance", MarketService.getBalanceDisplay(player));
            values.put("merchandises", String.valueOf(MarketService.getPlayerMerchandises(player).size()));
            values.put("mails", String.valueOf(MarketService.getMails(player).size()));
            ui.screen(player, ui.text.get("form.main.title"), ui.text.resolve(player, ui.text.get("form.main.content"), values))
                .button(ButtonKind.MARKET, ui.text.get("form.main.button-market"), () -> openMarketForm(player, MerchandiseType.SELLING, 0))
                .button(ButtonKind.PURCHASE, ui.text.get("form.main.button-purchase"), () -> openMarketForm(player, MerchandiseType.PURCHASING, 0))
                .button(ButtonKind.AUCTION, ui.text.get("form.main.button-auction"), () -> openMarketForm(player, MerchandiseType.BIDDING, 0))
                .button(ButtonKind.ALL, ui.text.get("form.main.button-all-items"), () -> openMarketForm(player, MerchandiseType.ANY, 0))
                .button(ButtonKind.OWN, ui.text.get("form.main.button-my-merchandises"), () -> openMyMerchandisesForm(player, 0))
                .button(ButtonKind.MAIL, ui.text.get("form.main.button-my-mailbox"), () -> openMailboxForm(player, 0))
                .button(ButtonKind.STORE, ui.text.get("form.main.button-my-store"), () -> profiles.store(player, merchant.getPlayerUUID(), 0))
                .button(ButtonKind.LISTING, ui.text.get("form.main.button-list-item"), () -> listings.open(player))
                .button(ButtonKind.SEARCH, I18n.text("search.title"), () -> market.search(player, new BrowseQuery(null, null, false, SearchFilter.EMPTY), () -> openMainForm(player)))
                .button(ButtonKind.ALL, I18n.text("stores.title"), () -> profiles.stores(player, 0))
                .button(ButtonKind.PROFILE, I18n.text("profile.title"), () -> profiles.profile(player))
                .button(ButtonKind.CATEGORY, I18n.text("profile.categories"), () -> profiles.categories(player, () -> openMainForm(player))).send();
        });
    }

    public void openMarketForm(Player player, MerchandiseType type, int page) {
        ui.safe(player, () -> market.browse(player, new BrowseQuery(type == MerchandiseType.ANY ? null : type, null, false, SearchFilter.EMPTY), page));
    }
    public void openMyMerchandisesForm(Player player, int page) {
        ui.safe(player, () -> market.browse(player, new BrowseQuery(null, player.getUniqueId(), true, SearchFilter.EMPTY), page));
    }
    public void openMailboxForm(Player player, int page) { ui.safe(player, () -> mail.open(player, player.getUniqueId(), MailForms.State.READY, page)); }
    public void openStoreForm(Player player) { ui.safe(player, () -> profiles.store(player, player.getUniqueId(), 0)); }
    public void openListingTypeForm(Player player) { ui.safe(player, () -> listings.open(player)); }
    public void openMarketDetailForm(Player player, Merchandise merchandise) {
        ui.safe(player, () -> market.detail(player, merchandise.getMerchandiseUID(), new BrowseQuery(null, null, false, SearchFilter.EMPTY), () -> openMainForm(player)));
    }
    public void openOwnDetailForm(Player player, Merchandise merchandise) {
        ui.safe(player, () -> market.edit(player, merchandise.getMerchandiseUID(), () -> openMyMerchandisesForm(player, 0)));
    }
    public void openMailDetailForm(Player player, ItemMail item) {
        ui.safe(player, () -> mail.detail(player, player.getUniqueId(), item.getMailUID(), () -> openMailboxForm(player, 0)));
    }
    void route(Player player, GmpRoute route) {
        Runnable home = () -> openMainForm(player);
        switch (route.kind) {
            case MAIN: openMainForm(player); break;
            case MARKET: openMarketForm(player, route.type, route.page); break;
            case OWN: market.browse(player, new BrowseQuery(route.type, player.getUniqueId(), true, SearchFilter.EMPTY), route.page); break;
            case MAIL:
                UUID owner = player.getUniqueId();
                if (route.target != null && !route.target.equalsIgnoreCase(player.getName())) {
                    GmpAccess.require(player, "Mailbox.GUI.For-Others");
                    owner = MarketService.merchant(route.target).getPlayerUUID();
                }
                mail.open(player, owner, MailForms.State.ALL, route.page); break;
            case PROFILE: profiles.profile(player); break;
            case CATEGORIES: profiles.categories(player, home); break;
            case STORES: profiles.stores(player, route.page); break;
            case STORE:
                UUID target = player.getUniqueId();
                if (route.target != null && !route.target.equalsIgnoreCase(player.getName())) {
                    GmpAccess.require(player, "Store.View.For-Others");
                    target = MarketService.merchant(route.target).getPlayerUUID();
                }
                profiles.store(player, target, route.page); break;
            case DETAIL: market.detail(player, route.uid, new BrowseQuery(null, null, false, SearchFilter.EMPTY), home); break;
            case EDIT: market.edit(player, route.uid, home); break;
            case CONTAINER: market.preview(player, route.uid, home); break;
            case LISTING: listings.open(player); break;
            case BULK_SELL: listings.bulk(player, false); break;
            case BULK_BUY: listings.bulk(player, true); break;
            case SEARCH: market.search(player, new BrowseQuery(null, null, false, SearchFilter.EMPTY), home); break;
            default: throw new IllegalArgumentException("Unknown menu route");
        }
    }
    void reload() {
        ui.reloadIcons();
        catalog = new MarketCatalog(ui.plugin);
        actions = new MarketActions(ui, catalog);
    }
    public void invalidate() { catalog.invalidate(); }
    public void transactionFinished(Player player, long uid) { actions.transactionFinished(player, uid); }
    public void cleanupPending(Player player) {
        listings.cleanup(player); ui.forget(player); actions.forget(player); catalog.forget(player);
    }
    public void shutdown() { listings.shutdown(); ui.clear(); actions.clear(); catalog.invalidate(); }
}
