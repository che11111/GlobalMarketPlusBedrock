package dev.che1s.globalmarketplusbedrock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;

/** Shared pagination, button/action binding and one-use form sessions. */
final class FormUi {
    final GlobalMarketPlusBedrock plugin;
    final FormText text;
    private volatile ItemIconResolver icons;
    private volatile ButtonAppearance appearance;
    private final FormSessions<UUID> sessions = new FormSessions<>();
    Consumer<Player> onOpen = player -> {};

    FormUi(GlobalMarketPlusBedrock plugin) {
        this.plugin = plugin;
        text = new FormText(plugin);
        icons = new ItemIconResolver(plugin);
        appearance = new ButtonAppearance(plugin.getConfig());
    }

    void reloadIcons() {
        icons = new ItemIconResolver(plugin);
        appearance = new ButtonAppearance(plugin.getConfig());
    }

    boolean allowed(Player player) {
        if (!plugin.isEnabled() || !player.isOnline()) return false;
        if (!player.hasPermission("globalmarketplusbedrock.use")) {
            text.message(player, "messages.no-permission");
            return false;
        }
        if (!BedrockUtil.isBedrock(player)) {
            text.message(player, "messages.bedrock-only");
            return false;
        }
        return true;
    }

    void safe(Player player, Runnable action) {
        if (!allowed(player)) return;
        try {
            GmpAccess.environment(player);
            action.run();
        } catch (MarketException ex) {
            if (ex.getCause() != null) RateLimitedLog.failure(ex.getMessage(), ex.getCause());
            player.sendMessage("§c" + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            text.message(player, "messages.invalid-number");
        } catch (RuntimeException | LinkageError ex) {
            RateLimitedLog.failure(I18n.text("logs.form-action-failed"), ex);
            text.message(player, "messages.data-error");
        }
    }

    boolean active(Player player, long token) { return sessions.active(player.getUniqueId(), token); }
    void forget(Player player) { sessions.forget(player.getUniqueId()); }
    void clear() { sessions.clear(); }

    private long begin(Player player) {
        onOpen.accept(player);
        return sessions.begin(player.getUniqueId());
    }

    private void respond(Player player, long token, Runnable action) {
        Scheduler.runTask(player, () -> {
            if (sessions.consume(player.getUniqueId(), token)) safe(player, action);
        });
    }

    Screen screen(Player player, String title, String content) { return new Screen(player, title, content); }

    final class Screen {
        private final Player player;
        private final SimpleForm.Builder form;
        private final List<Runnable> actions = new ArrayList<>();
        Screen(Player player, String title, String content) {
            this.player = player;
            form = SimpleForm.builder().title(text.plain(player, title)).content(text.plain(player, content));
        }
        Screen button(ButtonKind kind, String label, Runnable action) {
            form.button(appearance.function(kind, text.plain(player, label)), appearance.image(kind));
            actions.add(action);
            return this;
        }
        Screen product(String label, ItemStack item, Runnable action) {
            form.button(appearance.product(text.plain(player, label)), icons.resolve(item));
            actions.add(action);
            return this;
        }
        long send() {
            long token = begin(player);
            form.validResultHandler(response -> {
                int id = response.clickedButtonId();
                respond(player, token, () -> {
                    if (id >= 0 && id < actions.size()) actions.get(id).run();
                });
            });
            form.closedOrInvalidResultHandler(() -> respond(player, token, () -> {}));
            if (!BedrockUtil.sendForm(player, form)) {
                sessions.consume(player.getUniqueId(), token);
                text.message(player, "messages.form-failed");
            }
            return token;
        }
    }

    void custom(Player player, CustomForm.Builder form, Consumer<CustomFormResponse> action, Runnable back) {
        long token = begin(player);
        Consumer<CustomFormResponse> navigate = CustomFormNavigation.attach(form, text.get("navigation.action"),
            appearance.function(ButtonKind.CONFIRM, text.get("navigation.submit")),
            appearance.function(ButtonKind.BACK, text.get("common.back")), action, back);
        form.validResultHandler(response -> respond(player, token, () -> navigate.accept(response)));
        form.closedOrInvalidResultHandler(() -> respond(player, token, back));
        if (!BedrockUtil.sendForm(player, form)) {
            sessions.consume(player.getUniqueId(), token);
            text.message(player, "messages.form-failed");
        }
    }

    void input(Player player, String title, String label, String initial, Consumer<String> action, Runnable back) {
        custom(player, CustomForm.builder().title(text.plain(player, title))
            .input(text.plain(player, label), "", initial), response -> {
                String value = response.asInput();
                try {
                    action.accept(value == null ? "" : value.trim());
                } catch (IllegalArgumentException ex) {
                    text.message(player, "messages.invalid-number");
                    input(player, title, label, value == null ? "" : value, action, back);
                }
            }, back);
    }

    void confirm(Player player, String title, String content, Runnable yes, Runnable back) {
        screen(player, title, content).button(ButtonKind.CONFIRM, text.get("form.common.confirm", I18n.text("common.confirm")), yes)
            .button(ButtonKind.CANCEL, text.get("form.common.cancel", I18n.text("common.cancel")), back).send();
    }

    <T> Screen list(Player player, String title, String content, List<T> all, int requested,
            Function<T, String> label, Function<T, ItemStack> item, BiConsumer<T, Integer> click,
            IntConsumer page, Runnable back, Runnable invalidate) {
        return list(player, title, content, all, requested, label, item, null, click, page, back, invalidate);
    }

    <T> Screen list(Player player, String title, String content, List<T> all, int requested,
            Function<T, String> label, Function<T, ItemStack> item, ButtonKind entryKind, BiConsumer<T, Integer> click,
            IntConsumer page, Runnable back, Runnable invalidate) {
        PageWindow<T> window = PageWindow.of(all, requested, plugin.getConfig().getInt("page-size", 20));
        Screen screen = screen(player, title + " (" + (window.page + 1) + "/" + window.pages + ")",
            content + (all.isEmpty() ? "\n" + text.get("form.list.empty") : ""));
        screen.button(ButtonKind.BACK, text.get("form.list.button-back"), back);
        for (T value : window.items) {
            Runnable action = () -> click.accept(value, window.page);
            if (entryKind == null) screen.product(label.apply(value), item.apply(value), action);
            else screen.button(entryKind, label.apply(value), action);
        }
        if (window.page > 0) screen.button(ButtonKind.PREVIOUS, text.get("form.list.button-prev"), () -> page.accept(window.page - 1));
        if (window.page + 1 < window.pages) screen.button(ButtonKind.NEXT, text.get("form.list.button-next"), () -> page.accept(window.page + 1));
        screen.button(ButtonKind.REFRESH, text.get("form.common.refresh", I18n.text("common.refresh")), () -> { invalidate.run(); page.accept(window.page); });
        return screen;
    }
}
