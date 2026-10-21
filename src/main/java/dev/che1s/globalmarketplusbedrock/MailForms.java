package dev.che1s.globalmarketplusbedrock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import studio.trc.bukkit.globalmarketplus.api.Mailbox;
import studio.trc.bukkit.globalmarketplus.api.Merchant;
import studio.trc.bukkit.globalmarketplus.mailbox.ItemMail;
import studio.trc.bukkit.globalmarketplus.mailbox.ItemMailSort;
import studio.trc.bukkit.globalmarketplus.tool.Category;

final class MailForms {
    enum State { READY, ALL, EXPIRED, PICKED_UP }
    private final FormManager manager;
    private final FormUi ui;
    MailForms(FormManager manager) { this.manager = manager; ui = manager.ui; }

    private void access(Player player, UUID owner) {
        GmpAccess.require(player, "Mailbox.GUI");
        if (!owner.equals(player.getUniqueId())) GmpAccess.require(player, "Mailbox.GUI.For-Others");
    }

    void open(Player player, UUID owner, State state, int page) {
        access(player, owner);
        Merchant viewer = MarketService.getMerchant(player);
        Category category = viewer.getProfile().getCategory();
        List<ItemMail> list = new ArrayList<>();
        for (ItemMail mail : MarketService.mails(owner)) {
            if (mail == null || (category != null && !category.inCategory(mail))) continue;
            if (state == State.READY && (mail.isPickedUp() || mail.isExpired())) continue;
            if (state == State.EXPIRED && !mail.isExpired()) continue;
            if (state == State.PICKED_UP && !mail.isPickedUp()) continue;
            list.add(mail);
        }
        ItemMailSort sort = viewer.getProfile().getItemMailSort();
        if (sort == null) sort = ItemMailSort.getDefaultSort();
        if ("RANDOM".equals(sort.getName())) Collections.shuffle(list);
        else {
            final ItemMailSort selected = sort;
            list.sort((a, b) -> { int order = a.compareTo(b, selected); return order == 0 ? Long.compare(b.getMailUID(), a.getMailUID()) : order; });
        }
        String name = MarketService.require(Mailbox.getMailbox(owner), I18n.text("errors.mailbox-not-loaded")).getOwnerName();
        FormUi.Screen screen = ui.list(player, ui.text.get("form.list.mailbox-title"), I18n.text("labels.mailbox") + name + I18n.text("labels.show-line") + label(state),
            list, page, mail -> ui.text.resolve(player, ui.text.get("form.list.button-mail"), placeholders(mail))
                + (mail.isPickedUp() ? I18n.text("mail.picked-suffix") : mail.isExpired() ? I18n.text("mail.expired-suffix") : ""), ItemMail::getItem,
            (mail, current) -> detail(player, owner, mail.getMailUID(), () -> open(player, owner, state, current)),
            next -> open(player, owner, state, next), () -> manager.openMainForm(player), manager::invalidate);
        for (State value : State.values()) if (value != state) screen.button(ButtonKind.MAIL, I18n.text("labels.show") + label(value), () -> open(player, owner, value, 0));
        screen.button(ButtonKind.COLLECT, I18n.text("mail.pickup-all"), () -> ui.confirm(player, I18n.text("mail.pickup-all-title"), I18n.text("mail.pickup-all-confirm"),
            () -> manager.actions.submit(player, "gmp mailbox pickup all " + name, () -> open(player, owner, state, page)),
            () -> open(player, owner, state, page)));
        screen.button(ButtonKind.DELETE, I18n.text("mail.delete-all"), () -> ui.confirm(player, I18n.text("mail.delete-all-title"), I18n.text("mail.delete-all-confirm"),
            () -> manager.actions.submit(player, "gmp mailbox delete all " + name + " confirm", () -> open(player, owner, state, page)),
            () -> open(player, owner, state, page)));
        screen.button(ButtonKind.SORT, I18n.text("profile.mail-sort"), () -> manager.profiles.sorts(player, true, () -> open(player, owner, state, page)));
        screen.button(ButtonKind.CATEGORY, I18n.text("profile.categories"), () -> manager.profiles.categories(player, () -> open(player, owner, state, page))).send();
    }

    private ItemMail live(Player player, UUID owner, long uid) {
        access(player, owner);
        ItemMail mail = MarketService.mail(owner, uid);
        if (!owner.equals(mail.getOwnerUUID())) throw new MarketException(I18n.text("errors.mail-owner-mismatch"));
        return mail;
    }

    void detail(Player player, UUID owner, long uid, Runnable back) {
        ItemMail mail = live(player, owner, uid);
        String name = Mailbox.getMailbox(owner).getOwnerName();
        FormUi.Screen screen = ui.screen(player, ui.text.get("form.mail-detail.title"), ui.text.resolve(player,
            ui.text.get("form.mail-detail.content"), placeholders(mail)) + I18n.text("labels.status") + (mail.isPickedUp() ? I18n.text("mail.picked") : mail.isExpired() ? I18n.text("common.expired") : I18n.text("mail.ready"))
            + I18n.text("labels.description") + (mail.getDescription() == null ? I18n.text("common.none") : mail.getDescription()));
        if (!mail.isPickedUp() && !mail.isExpired()) screen.button(ButtonKind.COLLECT, ui.text.get("form.mail-detail.button-pickup"), () -> {
            live(player, owner, uid);
            manager.actions.submit(player, "gmp mailbox pickup " + uid + " " + name, back);
        });
        screen.button(ButtonKind.DELETE, I18n.text("mail.delete"), () -> ui.confirm(player, I18n.text("mail.delete-title"), I18n.text("mail.delete-confirm"),
            () -> manager.actions.submit(player, "gmp mailbox delete " + uid + " " + name + " confirm", back),
            () -> detail(player, owner, uid, back)));
        if (ContainerForms.container(mail.getItem())) screen.button(ButtonKind.CONTAINER, I18n.text("container.view"), () -> manager.containers.open(player,
            () -> live(player, owner, uid).getItem(), () -> detail(player, owner, uid, back)));
        screen.button(ButtonKind.BACK, ui.text.get("form.mail-detail.button-back"), back).send();
    }

    private String label(State state) {
        return state == State.READY ? I18n.text("mail.ready") : state == State.ALL ? I18n.text("common.all") : state == State.EXPIRED ? I18n.text("common.expired") : I18n.text("mail.picked");
    }
    private Map<String, String> placeholders(ItemMail mail) {
        Map<String, String> p = new HashMap<>();
        p.put("name", MarketService.getItemName(mail.getItem()));
        p.put("amount", mail.getItem() == null ? "0" : String.valueOf(mail.getItem().getAmount()));
        p.put("uid", String.valueOf(mail.getMailUID()));
        p.put("sender", mail.getSenderName() == null ? I18n.text("common.unknown") : mail.getSenderName());
        p.put("source", MarketService.getMailSource(mail));
        p.put("expire", MarketService.formatRemaining(mail.getExpireTime(), false));
        return p;
    }
}
