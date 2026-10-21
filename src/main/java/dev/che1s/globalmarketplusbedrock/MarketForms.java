package dev.che1s.globalmarketplusbedrock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import studio.trc.bukkit.globalmarketplus.api.Merchant;
import studio.trc.bukkit.globalmarketplus.merchandise.Merchandise;
import studio.trc.bukkit.globalmarketplus.merchandise.MerchandiseType;
import studio.trc.bukkit.globalmarketplus.merchandise.auction.Auction;

final class MarketForms {
    private final FormManager manager;
    private final FormUi ui;
    MarketForms(FormManager manager) { this.manager = manager; ui = manager.ui; }

    private void access(Player player, BrowseQuery query) {
        if (query.own) GmpAccess.require(player, "Merchandise.List");
        else if (query.owner != null) {
            GmpAccess.require(player, "Store.View");
            if (!query.owner.equals(player.getUniqueId())) {
                GmpAccess.require(player, "Store.View.For-Others");
                if (MarketService.merchant(query.owner).getStore().isStoreClosed()) throw new MarketException(I18n.text("errors.store-closed"));
            }
        } else GmpAccess.require(player, "Market.GUI");
        if (!query.filter.equals(SearchFilter.EMPTY)) GmpAccess.require(player, "Market.Search");
    }

    void browse(Player player, BrowseQuery query, int page) {
        browse(player, query, page, () -> manager.openMainForm(player));
    }

    void browse(Player player, BrowseQuery query, int page, Runnable exit) {
        access(player, query);
        List<Merchandise> all = manager.catalog.list(player, query);
        String title = query.own ? ui.text.get("form.list.my-title") : query.owner != null
            ? MarketService.merchant(query.owner).getStore().getDisplayStoreName()
            : query.type == MerchandiseType.PURCHASING ? ui.text.get("form.list.purchase-title")
            : query.type == MerchandiseType.BIDDING ? ui.text.get("form.list.auction-title") : ui.text.get("form.list.market-title");
        String category = MarketService.getMerchant(player).getProfile().getCategory() == null ? I18n.text("common.all")
            : MarketService.getMerchant(player).getProfile().getCategory().getDisplayName();
        FormUi.Screen screen = ui.list(player, title, I18n.text("labels.category") + category, all, page,
            m -> ui.text.resolve(player, ui.text.get(query.own ? "form.list.button-my-item"
                : m.getMerchandiseType() == MerchandiseType.BIDDING ? "form.list.button-auction" : "form.list.button-item"), placeholders(m)),
            Merchandise::getItem, (m, current) -> {
                Runnable back = () -> browse(player, query, current, exit);
                if (query.own) edit(player, m.getMerchandiseUID(), back);
                else detail(player, m.getMerchandiseUID(), query, back);
            }, next -> browse(player, query, next, exit), exit, manager.catalog::invalidate);
        screen.button(ButtonKind.CATEGORY, I18n.text("profile.categories"), () -> manager.profiles.categories(player, () -> browse(player, query, page, exit)));
        screen.button(ButtonKind.SORT, I18n.text("market.sort"), () -> manager.profiles.sorts(player, false, () -> browse(player, query, page, exit)));
        screen.button(ButtonKind.SEARCH, I18n.text("market.search"), () -> search(player, query, () -> browse(player, query, page, exit)));
        if (!query.filter.equals(SearchFilter.EMPTY)) screen.button(ButtonKind.CANCEL, I18n.text("market.search-clear"), () -> browse(player,
            new BrowseQuery(query.type, query.owner, query.own, SearchFilter.EMPTY), 0, exit));
        screen.send();
    }

    private Merchandise visible(Player player, long uid, BrowseQuery origin) {
        access(player, origin);
        Merchandise item = MarketService.live(uid);
        if (origin.owner != null && !origin.owner.equals(item.getOwnerUUID())) throw new MarketException(I18n.text("errors.wrong-store"));
        if (!player.getUniqueId().equals(item.getOwnerUUID())
                && !MarketService.publicListing(item, MarketService.merchant(item.getOwnerUUID()))) throw new MarketException(I18n.text("errors.item-hidden"));
        if (!GmpAccess.rule(player, "OtherPermissions.yml", "Currencies." + MarketService.currency(item))) throw new MarketException(I18n.text("errors.currency-denied"));
        return item;
    }

    void preview(Player player, long uid, Runnable back) {
        BrowseQuery origin = new BrowseQuery(null, null, false, SearchFilter.EMPTY);
        Merchandise item = visible(player, uid, origin);
        GmpAccess.require(player, item.getItem().getType().name().endsWith("SHULKER_BOX") ? "Merchandise.Shulker-Box" : "Merchandise.Bundle");
        manager.containers.open(player, () -> previewItem(player, uid, origin), back);
    }

    private org.bukkit.inventory.ItemStack previewItem(Player player, long uid, BrowseQuery origin) {
        Merchandise item = visible(player, uid, origin);
        requireContainer(player, item);
        return item.getItem();
    }

    private void requireContainer(Player player, Merchandise item) {
        GmpAccess.require(player, item.getItem().getType().name().endsWith("SHULKER_BOX") ? "Merchandise.Shulker-Box" : "Merchandise.Bundle");
    }

    void detail(Player player, long uid, BrowseQuery origin, Runnable back) {
        Merchandise item = visible(player, uid, origin);
        GmpAccess.require(player, item instanceof Auction ? "Auction.GUI" : "Transaction.GUI");
        Map<String, String> values = placeholders(item);
        String content = ui.text.resolve(player, ui.text.get(item instanceof Auction
            ? "form.detail.content-auction" : "form.detail.content"), values);
        FormUi.Screen screen = ui.screen(player, ui.text.get("form.detail.title"), content + I18n.text("labels.currency") + MarketService.currency(item)
            + "\n\n" + ContainerForms.describe(item.getItem()));
        if (item instanceof Auction) {
            screen.button(ButtonKind.AUCTION, ui.text.get("form.detail.button-bid"), () -> bid(player, uid, origin, back, ((Auction) item).getBiddingPrice() + 1));
        } else {
            if (item.isAllowWholesale()) screen.button(ButtonKind.CONFIRM, ui.text.get(item.getMerchandiseType() == MerchandiseType.SELLING
                ? "form.detail.button-buy-wholesale" : "form.detail.button-sell-wholesale"), () -> transaction(player, uid, origin, null, back));
            if (item.isAllowRetail()) screen.button(ButtonKind.CONFIRM, ui.text.get(item.getMerchandiseType() == MerchandiseType.SELLING
                ? "form.detail.button-buy-retail" : "form.detail.button-sell-retail"), () -> amount(player, uid, origin, back));
        }
        if (ContainerForms.container(item.getItem())) screen.button(ButtonKind.CONTAINER, I18n.text("container.view"), () -> {
            GmpAccess.require(player, item.getItem().getType().name().endsWith("SHULKER_BOX") ? "Merchandise.Shulker-Box" : "Merchandise.Bundle");
            manager.containers.open(player, () -> previewItem(player, uid, origin), () -> detail(player, uid, origin, back));
        });
        if (player.getUniqueId().equals(item.getOwnerUUID()) || GmpAccess.command(player, "Merchandise.GUI.For-Others")) {
            screen.button(ButtonKind.OPTIONS, I18n.text("market.advanced"), () -> edit(player, uid, () -> detail(player, uid, origin, back)));
        }
        screen.button(ButtonKind.SEARCH, I18n.text("market.similar"), () -> browse(player, new BrowseQuery(item.getMerchandiseType(), null, false,
            new SearchFilter(item.getItem().getType().name(), "", "", "", "", "", MarketService.currency(item), "")),
            0, () -> detail(player, uid, origin, back)));
        screen.button(ButtonKind.REFRESH, ui.text.get("form.common.refresh", I18n.text("common.refresh")), () -> { manager.catalog.invalidate(); detail(player, uid, origin, back); });
        screen.button(ButtonKind.BACK, ui.text.get("form.detail.button-back"), back).send();
    }

    private void amount(Player player, long uid, BrowseQuery origin, Runnable back) {
        ui.input(player, I18n.text("market.retail-amount"), I18n.text("market.retail-amount-label"), "1", raw -> {
            int amount = NumberInput.amount(raw);
            if (amount <= 0) throw new IllegalArgumentException("Positive amount required");
            transaction(player, uid, origin, amount, back);
        }, () -> detail(player, uid, origin, back));
    }

    private void transaction(Player player, long uid, BrowseQuery origin, Integer amount, Runnable back) {
        Merchandise quote = visible(player, uid, origin);
        if (quote instanceof Auction) throw new MarketException(I18n.text("errors.auction-use-bid"));
        if (amount == null ? !quote.isAllowWholesale() : !quote.isAllowRetail()) throw new MarketException(I18n.text("errors.mode-closed"));
        TransactionQuote snapshot = new TransactionQuote(quote, amount);
        double unit = snapshot.price;
        String total = amount == null ? MarketService.formatMoney(unit)
            : BigDecimal.valueOf(unit).multiply(BigDecimal.valueOf(amount)).stripTrailingZeros().toPlainString();
        ui.confirm(player, I18n.text("market.confirm-title"), I18n.text("labels.product") + MarketService.getItemName(quote.getItem()) + I18n.text("labels.amount")
            + (amount == null ? quote.getItem().getAmount() : amount) + I18n.text("labels.total") + total + " " + MarketService.currency(quote)
            + I18n.text("market.tax-note"), () -> {
                Merchandise current = visible(player, uid, origin);
                snapshot.validate(current);
                if (amount != null && !current.getMerchandiseOption().isUnlimited() && amount > current.getItem().getAmount()) {
                    throw new MarketException(I18n.text("errors.insufficient-stock"));
                }
                manager.actions.submit(player, "gmp transaction confirm " + uid + (amount == null ? "" : " " + amount), back, uid);
            }, () -> detail(player, uid, origin, back));
    }

    private void bid(Player player, long uid, BrowseQuery origin, Runnable back, double value) {
        Merchandise item = visible(player, uid, origin);
        if (!(item instanceof Auction) || !Double.isFinite(value)) throw new MarketException(I18n.text("errors.auction-unavailable"));
        FormUi.Screen screen = ui.screen(player, I18n.text("auction.bid-title"), I18n.text("labels.highest-bid") + ((Auction) item).getDisplayBiddingPrice()
            + I18n.text("labels.your-bid") + MarketService.formatMoney(value));
        for (int delta : new int[] {10, 100, 1000, -10, -100, -1000}) {
            screen.button(delta > 0 ? ButtonKind.ADD : ButtonKind.SUBTRACT, (delta > 0 ? "+" : "") + delta, () -> bid(player, uid, origin, back, Math.max(0, value + delta)));
        }
        screen.button(ButtonKind.EDIT, I18n.text("auction.enter-price"), () -> ui.input(player, I18n.text("auction.bid-title"), I18n.text("auction.price-label"), MarketService.formatMoney(value),
            raw -> bid(player, uid, origin, back, NumberInput.decimal(raw)), () -> bid(player, uid, origin, back, value)));
        screen.button(ButtonKind.AUCTION, I18n.text("auction.confirm-bid"), () -> {
            if (value <= 0) throw new IllegalArgumentException("Positive bid required");
            visible(player, uid, origin);
            manager.actions.submit(player, "gmp auction bid " + uid + " " + MarketService.formatMoney(value),
                () -> detail(player, uid, origin, back));
        });
        screen.button(ButtonKind.BACK, I18n.text("common.back"), () -> detail(player, uid, origin, back)).send();
    }

    void edit(Player player, long uid, Runnable back) {
        GmpAccess.require(player, "Merchandise.GUI");
        Merchandise item = MarketService.live(uid);
        boolean own = player.getUniqueId().equals(item.getOwnerUUID());
        if (!own) GmpAccess.require(player, "Merchandise.GUI.For-Others");
        FormUi.Screen screen = ui.screen(player, I18n.text("market.advanced"), ui.text.resolve(player,
            ui.text.get(item instanceof Auction ? "form.detail.content-auction" : "form.detail.content"), placeholders(item)));
        if (!(item instanceof Auction)) {
            screen.button(ButtonKind.EDIT, I18n.text("market.set-wholesale"), () -> reprice(player, uid, false, back));
            screen.button(ButtonKind.EDIT, I18n.text("market.set-retail"), () -> reprice(player, uid, true, back));
        }
        String[] options = {"neverexpire", "unlimited", "hidemerchantname", "hidemerchandise"};
        String[] names = {I18n.text("options.never-expire"), I18n.text("options.unlimited"), I18n.text("options.hide-name"), I18n.text("options.hide-items")};
        boolean[] states = {item.getMerchandiseOption().isNeverExpire(), item.getMerchandiseOption().isUnlimited(),
            item.getMerchandiseOption().isHiddenMerchantName(), item.getMerchandiseOption().isHiddenMerchandise()};
        for (int i = 0; i < options.length; i++) {
            final String option = options[i];
            screen.button(ButtonKind.OPTIONS, names[i] + " [" + (states[i] ? I18n.text("common.on") : I18n.text("common.off")) + "]", () -> manager.actions.submit(player,
                "gmp merchandise set " + uid + " " + option + " switch", () -> edit(player, uid, back)));
        }
        if (item instanceof Auction) screen.button(ButtonKind.AUCTION, I18n.text("auction.early-confirm"), () -> ui.confirm(player, I18n.text("auction.early-title"),
            I18n.text("auction.early-help"), () -> {
                MarketService.live(uid);
                manager.actions.submit(player, "gmp auction confirm " + uid, back);
            }, () -> edit(player, uid, back)));
        screen.button(ButtonKind.DELETE, ui.text.get("form.detail.button-remove"), () -> ui.confirm(player, I18n.text("market.remove-title"), I18n.text("market.remove-help"),
            () -> manager.actions.submit(player, "gmp merchandise remove " + uid, back), () -> edit(player, uid, back)));
        if (ContainerForms.container(item.getItem())) screen.button(ButtonKind.CONTAINER, I18n.text("container.view"), () -> manager.containers.open(player,
            () -> {
                GmpAccess.require(player, "Merchandise.GUI");
                Merchandise current = MarketService.live(uid);
                if (!player.getUniqueId().equals(current.getOwnerUUID())) GmpAccess.require(player, "Merchandise.GUI.For-Others");
                requireContainer(player, current);
                return current.getItem();
            }, () -> edit(player, uid, back)));
        screen.button(ButtonKind.BACK, I18n.text("common.back"), back).send();
    }

    private void reprice(Player player, long uid, boolean retail, Runnable back) {
        Merchandise item = MarketService.live(uid);
        ui.input(player, retail ? I18n.text("market.retail-price-title") : I18n.text("market.wholesale-price-title"), I18n.text("market.price-label"), MarketService.formatMoney(
            retail ? item.getRetailPrice() : item.getPrice()), raw -> {
                double value = NumberInput.decimal(raw);
                if (value < 0 && value != -1) throw new IllegalArgumentException("Invalid price");
                manager.actions.submit(player, "gmp merchandise set " + uid + (retail ? " retailprice " : " price ")
                    + MarketService.formatMoney(value), () -> edit(player, uid, back));
            }, () -> edit(player, uid, back));
    }

    void search(Player player, BrowseQuery origin, Runnable back) {
        SearchFilter f = origin.filter;
        search(player, origin, back, new String[] {f.item, f.seller, f.name, f.lore,
            f.min == null ? "" : MarketService.formatMoney(f.min), f.max == null ? "" : MarketService.formatMoney(f.max),
            f.currency, f.category}, origin.type == MerchandiseType.SELLING ? 1 : origin.type == MerchandiseType.PURCHASING ? 2
                : origin.type == MerchandiseType.BIDDING ? 3 : 0);
    }

    private void search(Player player, BrowseQuery origin, Runnable back, String[] draft, int type) {
        GmpAccess.require(player, "Market.Search");
        Runnable returnToSearch = () -> search(player, origin, back, draft, type);
        FormUi.Screen screen = ui.screen(player, I18n.text("search.title"), I18n.text("search.help"));
        screen.button(ButtonKind.BACK, I18n.text("common.back"), back);
        String[] fields = {"material", "seller", "name", "lore", "min", "max", "currency", "category"};
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            String label = I18n.text("search." + fields[i]);
            screen.button(ButtonKind.EDIT, label + "\n" + (draft[i].isEmpty() ? I18n.text("common.all") : draft[i]),
                () -> ui.input(player, I18n.text("search.title"), label, draft[index], raw -> {
                    String[] updated = draft.clone();
                    updated[index] = raw;
                    search(player, origin, back, updated, type);
                }, returnToSearch));
        }
        List<String> types = Arrays.asList(I18n.text("common.all"), I18n.text("market.selling"),
            I18n.text("market.purchasing"), I18n.text("market.bidding"));
        screen.button(ButtonKind.CATEGORY, I18n.text("search.type") + ": " + types.get(type),
            () -> ui.custom(player, CustomForm.builder().title(I18n.text("search.type"))
                .dropdown(I18n.text("search.type"), types, type),
                response -> search(player, origin, back, draft, response.asDropdown(0)), returnToSearch));
        screen.button(ButtonKind.SEARCH, I18n.text("search.start"), () -> {
            try {
                SearchFilter filter = new SearchFilter(draft[0], draft[1], draft[2], draft[3], draft[4], draft[5], draft[6], draft[7]);
                MerchandiseType selected = type == 1 ? MerchandiseType.SELLING : type == 2 ? MerchandiseType.PURCHASING
                    : type == 3 ? MerchandiseType.BIDDING : null;
                browse(player, new BrowseQuery(selected, origin.owner, origin.own, filter), 0, returnToSearch);
            } catch (IllegalArgumentException ex) {
                ui.text.message(player, "messages.invalid-number");
                returnToSearch.run();
            }
        });
        screen.button(ButtonKind.CANCEL, I18n.text("market.search-clear"), () -> search(player, origin, back,
            new String[] {"", "", "", "", "", "", "", ""}, 0)).send();
    }

    Map<String, String> placeholders(Merchandise item) {
        Map<String, String> p = new HashMap<>();
        p.put("name", MarketService.getItemName(item.getItem()));
        p.put("type", MarketService.getTypeLabel(item));
        p.put("seller", MarketService.getSellerDisplay(item));
        p.put("price", item instanceof Auction || item.isAllowWholesale() ? item.getDisplayPrice() : I18n.text("common.none"));
        p.put("retail_price", item.isAllowRetail() ? item.getDisplayRetailPrice() : I18n.text("common.none"));
        p.put("amount", String.valueOf(item.getItem().getAmount()));
        p.put("uid", String.valueOf(item.getMerchandiseUID()));
        p.put("expire", MarketService.formatRemaining(item.getExpireTime(), MarketService.isNeverExpire(item)));
        Auction auction = MarketService.asAuction(item);
        p.put("bidding_price", auction == null ? I18n.text("common.none") : auction.getDisplayBiddingPrice());
        p.put("top_bidder", auction == null || auction.getTopBidderName() == null ? I18n.text("common.none") : auction.getTopBidderName());
        return p;
    }
}
