package dev.che1s.globalmarketplusbedrock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import studio.trc.bukkit.globalmarketplus.api.GlobalMarket;
import studio.trc.bukkit.globalmarketplus.api.Merchant;
import studio.trc.bukkit.globalmarketplus.mailbox.ItemMailSort;
import studio.trc.bukkit.globalmarketplus.merchandise.MerchandiseSort;
import studio.trc.bukkit.globalmarketplus.merchant.MerchantProfile;
import studio.trc.bukkit.globalmarketplus.merchant.MerchantStore;
import studio.trc.bukkit.globalmarketplus.tool.Category;

final class ProfileForms {
    private final FormManager manager;
    private final FormUi ui;
    ProfileForms(FormManager manager) { this.manager = manager; ui = manager.ui; }

    void profile(Player player) {
        GmpAccess.require(player, "Profile.GUI");
        Merchant merchant = MarketService.getMerchant(player);
        MerchantProfile p = merchant.getProfile();
        MerchantStore store = merchant.getStore();
        FormUi.Screen screen = ui.screen(player, I18n.text("profile.title"), I18n.text("labels.merchant") + merchant.getPlayerName() + I18n.text("labels.store")
            + store.getDisplayStoreName() + I18n.text("labels.balance") + MarketService.getBalanceDisplay(player)
            + I18n.text("labels.introduction") + String.join("\n", store.getStoreDescription()));
        screen.button(ButtonKind.EDIT, I18n.text("profile.rename"), () -> ui.input(player, I18n.text("profile.store-name"), I18n.text("profile.store-name-label"), store.getStoreName(),
            raw -> { validateText(raw); manager.actions.submit(player, "gmp profile set storename " + raw, () -> profile(player)); }, () -> profile(player)));
        screen.button(ButtonKind.EDIT, I18n.text("profile.description-edit"), () -> descriptions(player));
        String[] keys = {"hidemerchandises", "hidemerchantname", "storeclosed", "mailboxlocked"};
        String[] labels = {I18n.text("options.hide-items"), I18n.text("options.hide-name"), I18n.text("options.store-closed"), I18n.text("options.mailbox-locked")};
        boolean[] states = {p.isHiddenMerchandises(), p.isHiddenMerchantName(), store.isStoreClosed(), p.isMailboxLocked()};
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            screen.button(ButtonKind.OPTIONS, labels[i] + " [" + (states[i] ? I18n.text("common.on") : I18n.text("common.off")) + "]", () -> manager.actions.submit(player,
                "gmp profile set " + key + " switch", () -> profile(player)));
        }
        screen.button(ButtonKind.SORT, I18n.text("profile.item-sort"), () -> sorts(player, false, () -> profile(player)));
        screen.button(ButtonKind.SORT, I18n.text("profile.mail-sort"), () -> sorts(player, true, () -> profile(player)));
        screen.button(ButtonKind.CATEGORY, I18n.text("profile.item-category"), () -> categories(player, () -> profile(player)));
        screen.button(ButtonKind.REFRESH, I18n.text("profile.refresh"), () -> manager.actions.submit(player, "gmp profile refresh " + player.getName(), () -> profile(player)));
        screen.button(ButtonKind.STORE, I18n.text("profile.my-store"), () -> store(player, player.getUniqueId(), 0));
        screen.button(ButtonKind.BACK, I18n.text("common.back"), () -> manager.openMainForm(player)).send();
    }

    private void descriptions(Player player) { descriptions(player, 0); }

    private void descriptions(Player player, int requested) {
        GmpAccess.require(player, "Profile.Set");
        List<String> lines = new ArrayList<>(MarketService.getMerchant(player).getStore().getStoreDescription());
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) indexes.add(i);
        FormUi.Screen screen = ui.list(player, I18n.text("profile.description-edit"), I18n.text("profile.description-help"), indexes, requested,
            i -> (i + 1) + ": " + lines.get(i), i -> null, ButtonKind.EDIT,
            (index, page) -> descriptionLine(player, lines, index, page),
            next -> descriptions(player, next), () -> profile(player), manager::invalidate);
        screen.button(ButtonKind.ADD, I18n.text("profile.description-add"), () -> ui.input(player, I18n.text("profile.description-add-title"), I18n.text("profile.description-add-label"), "", raw -> {
            validateText(raw);
            manager.actions.submit(player, "gmp profile set storedescription add " + raw, () -> descriptions(player, requested));
        }, () -> descriptions(player, requested))).send();
    }

    private void descriptionLine(Player player, List<String> snapshot, int index, int page) {
        Runnable back = () -> descriptions(player, page);
        ui.screen(player, I18n.text("profile.description-line-prefix") + (index + 1) + I18n.text("profile.description-line-suffix"), snapshot.get(index))
            .button(ButtonKind.EDIT, I18n.text("common.edit"), () -> ui.input(player, I18n.text("profile.description-set-title"), I18n.text("profile.description-set-label"), snapshot.get(index), raw -> {
                validateText(raw);
                requireDescription(player, snapshot);
                manager.actions.submit(player, "gmp profile set storedescription set " + (index + 1) + " " + raw, back);
            }, back))
            .button(ButtonKind.DELETE, I18n.text("common.delete"), () -> ui.confirm(player, I18n.text("profile.description-delete-title"), snapshot.get(index), () -> {
                requireDescription(player, snapshot);
                manager.actions.submit(player, "gmp profile set storedescription remove " + (index + 1), back);
            }, back)).button(ButtonKind.BACK, I18n.text("common.back"), back).send();
    }

    private void requireDescription(Player player, List<String> snapshot) {
        GmpAccess.require(player, "Profile.Set");
        if (!snapshot.equals(MarketService.getMerchant(player).getStore().getStoreDescription())) {
            throw new MarketException(I18n.text("errors.description-changed"));
        }
    }

    static void validateText(String text) {
        if (text == null || text.trim().isEmpty() || text.length() > 256 || text.contains("\n") || text.contains("\r")) {
            throw new MarketException(I18n.text("errors.single-line"));
        }
    }

    void categories(Player player, Runnable back) {
        GmpAccess.require(player, "Category.GUI");
        List<Category> allowed = new ArrayList<>();
        for (Category category : Category.getCategories()) if (category.hasPermission(player)) allowed.add(category);
        categoryPage(player, allowed, 0, back);
    }
    private void categoryPage(Player player, List<Category> categories, int page, Runnable back) {
        Category selected = MarketService.getMerchant(player).getProfile().getCategory();
        ui.list(player, I18n.text("profile.item-category"), I18n.text("profile.category-help"), categories, page,
            category -> category.getDisplayName() + (selected != null && selected.equals(category) ? I18n.text("common.selected-suffix") : ""), category -> null, ButtonKind.CATEGORY,
            (category, current) -> manager.actions.submit(player, "gmp category set " + category.getName(), () -> { manager.invalidate(); back.run(); }),
            next -> categoryPage(player, categories, next, back), back, manager::invalidate)
            .button(ButtonKind.CANCEL, I18n.text("profile.category-reset"), () -> manager.actions.submit(player, "gmp category reset", () -> { manager.invalidate(); back.run(); })).send();
    }

    void sorts(Player player, boolean mail, Runnable back) {
        GmpAccess.require(player, "Profile.Set");
        List<String> keys = new ArrayList<>(), names = new ArrayList<>();
        if (mail) for (ItemMailSort sort : ItemMailSort.values()) { keys.add(sort.getName()); names.add(sort.getDescription()); }
        else for (MerchandiseSort sort : MerchandiseSort.values()) { keys.add(sort.getName()); names.add(sort.getDescription()); }
        FormUi.Screen screen = ui.screen(player, mail ? I18n.text("profile.mail-sort") : I18n.text("profile.item-sort"), I18n.text("profile.sort-help"));
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            screen.button(ButtonKind.SORT, names.get(i), () -> manager.actions.submit(player, "gmp profile set " + (mail ? "itemmailsort" : "merchandisesort")
                + " set " + key, () -> { manager.invalidate(); back.run(); }));
        }
        screen.button(ButtonKind.CANCEL, I18n.text("profile.sort-reset"), () -> manager.actions.submit(player, "gmp profile set " + (mail ? "itemmailsort" : "merchandisesort")
            + " reset", () -> { manager.invalidate(); back.run(); })).button(ButtonKind.BACK, I18n.text("common.back"), back).send();
    }

    void stores(Player player, int page) {
        GmpAccess.require(player, "Store.List.GUI");
        List<MerchantStore> stores = new ArrayList<>();
        List<MerchantStore> source = GlobalMarket.getGlobalMarket().getMerchantStores();
        if (source != null) for (MerchantStore store : new ArrayList<>(source)) {
            if (store != null && (!store.isStoreClosed() || store.getOwnerUUID().equals(player.getUniqueId()))) stores.add(store);
        }
        stores.sort((a, b) -> a.getOwnerUUID().compareTo(b.getOwnerUUID()));
        ui.list(player, I18n.text("stores.title"), I18n.text("stores.help"), stores, page, store -> store.getDisplayStoreName()
            + " | " + store.getMerchant().getPlayerName(), store -> null, ButtonKind.STORE,
            (store, current) -> store(player, store.getOwnerUUID(), 0, () -> stores(player, current)), next -> stores(player, next),
            () -> manager.openMainForm(player), manager::invalidate).send();
    }

    void store(Player player, UUID owner, int page) {
        store(player, owner, page, () -> manager.openMainForm(player));
    }

    private void store(Player player, UUID owner, int page, Runnable back) {
        GmpAccess.require(player, "Store.View");
        boolean own = owner.equals(player.getUniqueId());
        if (!own) GmpAccess.require(player, "Store.View.For-Others");
        Merchant merchant = MarketService.merchant(owner);
        MerchantStore store = merchant.getStore();
        if (!own && store.isStoreClosed()) throw new MarketException(I18n.text("errors.store-closed"));
        java.util.Map<String, String> values = new java.util.HashMap<>();
        values.put("store_name", store.getDisplayStoreName());
        values.put("store_description", String.join("\n", store.getStoreDescription()));
        values.put("balance", own ? MarketService.getBalanceDisplay(player) : I18n.text("stores.owner-only"));
        values.put("merchandises", String.valueOf(merchant.getMerchandises().size()));
        values.put("sold", String.valueOf(store.getStatistics().getTotalSoldNumber()));
        values.put("purchased", String.valueOf(store.getStatistics().getTotalPurchasedItems()));
        values.put("auction", String.valueOf(store.getStatistics().getTotalAuctionNumber()));
        FormUi.Screen screen = ui.screen(player, store.getDisplayStoreName(), ui.text.resolve(player, ui.text.get("form.store.content"), values));
        screen.button(ButtonKind.ALL, I18n.text("stores.browse"), () -> manager.market.browse(player,
            new BrowseQuery(null, owner, false, SearchFilter.EMPTY), page, () -> store(player, owner, page, back)));
        if (own) screen.button(ButtonKind.PROFILE, I18n.text("stores.edit-profile"), () -> profile(player));
        screen.button(ButtonKind.REFRESH, I18n.text("common.refresh"), () -> { manager.invalidate(); store(player, owner, page, back); });
        screen.button(ButtonKind.BACK, ui.text.get("form.store.button-back"), back).send();
    }
}
