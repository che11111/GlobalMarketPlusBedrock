package dev.che1s.globalmarketplusbedrock;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/** Dispatches commands without claiming that a dispatched command succeeded. */
final class MarketActions {
    private final FormUi ui;
    private final MarketCatalog catalog;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private static final class Pending {
        final long uid, screen;
        final Runnable back;
        Pending(long uid, long screen, Runnable back) { this.uid = uid; this.screen = screen; this.back = back; }
    }
    MarketActions(FormUi ui, MarketCatalog catalog) { this.ui = ui; this.catalog = catalog; }

    void submit(Player player, String command, Runnable back) { submit(player, command, back, null); }

    void submit(Player player, String command, Runnable back, Long transactionUid) {
        catalog.invalidate();
        long token = ui.screen(player, ui.text.get("form.common.processing", I18n.text("common.processing")),
            ui.text.get("form.common.processing-content", I18n.text("common.processing-help")))
            .button(ButtonKind.REFRESH, ui.text.get("form.common.refresh", I18n.text("common.refresh")), () -> { catalog.invalidate(); pending.remove(player.getUniqueId()); back.run(); })
            .button(ButtonKind.BACK, ui.text.get("form.common.back", I18n.text("common.back")), () -> { pending.remove(player.getUniqueId()); back.run(); }).send();
        pending.remove(player.getUniqueId());
        if (transactionUid != null) pending.put(player.getUniqueId(), new Pending(transactionUid, token, back));
        try {
            if (!MarketService.command(player, command)) throw new MarketException(I18n.text("errors.command-dispatch"));
        } catch (RuntimeException | LinkageError ex) {
            pending.remove(player.getUniqueId());
            throw ex;
        }
    }

    void transactionFinished(Player player, long uid) {
        catalog.invalidate();
        Pending action = pending.get(player.getUniqueId());
        if (action == null || action.uid != uid) return;
        Scheduler.runTask(player, () -> {
            if (pending.remove(player.getUniqueId(), action) && ui.active(player, action.screen)) {
                ui.safe(player, action.back);
            }
        });
    }
    void forget(Player player) { pending.remove(player.getUniqueId()); }
    void clear() { pending.clear(); }
}
