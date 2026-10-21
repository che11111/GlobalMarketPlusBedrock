package dev.che1s.globalmarketplusbedrock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/** Read-only copies of container contents; never exposes a movable Inventory. */
final class ContainerForms {
    private final FormUi ui;
    static final class Entry {
        final int slot;
        final ItemStack item;
        Entry(int slot, ItemStack item) { this.slot = slot; this.item = item.clone(); }
    }
    ContainerForms(FormUi ui) { this.ui = ui; }

    static boolean container(ItemStack item) {
        if (item == null) return false;
        return item.getType().name().endsWith("SHULKER_BOX") || isBundleMeta(item.getItemMeta());
    }

    static List<Entry> contents(ItemStack item) {
        List<Entry> result = new ArrayList<>();
        ItemMeta meta = item.clone().getItemMeta();
        if (meta instanceof BlockStateMeta) {
            BlockState state = ((BlockStateMeta) meta).getBlockState();
            if (state instanceof ShulkerBox) {
                ItemStack[] stacks = ((ShulkerBox) state).getInventory().getContents();
                for (int i = 0; i < stacks.length; i++) {
                    if (stacks[i] != null && !stacks[i].getType().isAir()) result.add(new Entry(i, stacks[i]));
                }
            }
        } else if (isBundleMeta(meta)) {
            List<ItemStack> stacks = bundleContents(meta);
            for (int i = 0; i < stacks.size(); i++) result.add(new Entry(i, stacks.get(i)));
        }
        return result;
    }

    /** BundleMeta was added after the 1.16 API baseline, so resolve it only when present. */
    private static Class<?> bundleMetaType() {
        try {
            return Class.forName("org.bukkit.inventory.meta.BundleMeta", false, ContainerForms.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError unavailable) {
            return null;
        }
    }

    private static boolean isBundleMeta(ItemMeta meta) {
        Class<?> type = bundleMetaType();
        return meta != null && type != null && type.isInstance(meta);
    }

    private static List<ItemStack> bundleContents(ItemMeta meta) {
        Class<?> type = bundleMetaType();
        if (meta == null || type == null || !type.isInstance(meta)) return java.util.Collections.emptyList();
        try {
            Method getItems = type.getMethod("getItems");
            Object contents = getItems.invoke(meta);
            if (contents instanceof List<?>) {
                List<ItemStack> result = new ArrayList<>();
                for (Object item : (List<?>) contents) if (item instanceof ItemStack) result.add((ItemStack) item);
                return result;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Bundles are an optional display feature on server versions that expose BundleMeta.
        }
        return java.util.Collections.emptyList();
    }

    void open(Player player, Supplier<ItemStack> source, Runnable back) { open(player, source, back, 0, 0, () -> source.get()); }

    private void open(Player player, Supplier<ItemStack> source, Runnable back, int depth, int page, Runnable authorize) {
        authorize.run();
        ItemStack item = source.get();
        if (!container(item)) throw new MarketException(I18n.text("errors.not-container"));
        List<Entry> entries = contents(item);
        ui.list(player, I18n.text("container.title") + MarketService.getItemName(item), I18n.text("container.readonly-prefix") + entries.size() + I18n.text("container.readonly-suffix"),
            entries, page, e -> I18n.text("container.slot") + (e.slot + 1) + " | " + MarketService.getItemName(e.item) + " x" + e.item.getAmount(),
            e -> e.item, (e, current) -> inspect(player, e.item,
                () -> open(player, source, back, depth, current, authorize), depth, authorize),
            next -> open(player, source, back, depth, next, authorize), back, () -> {}).send();
    }

    private void inspect(Player player, ItemStack item, Runnable back, int depth, Runnable authorize) {
        authorize.run();
        FormUi.Screen screen = ui.screen(player, MarketService.getItemName(item), describe(item));
        int limit = Math.max(1, Math.min(8, ui.plugin.getConfig().getInt("container-preview-depth", 4)));
        if (container(item) && depth + 1 < limit) {
            screen.button(ButtonKind.CONTAINER, I18n.text("container.inner"), () -> open(player, () -> item.clone(), back, depth + 1, 0, authorize));
        }
        screen.button(ButtonKind.BACK, ui.text.get("form.common.back", I18n.text("common.back")), back).send();
    }

    static String describe(ItemStack item) {
        StringBuilder result = new StringBuilder(I18n.text("labels.item")).append(MarketService.getItemName(item))
            .append(I18n.text("labels.material")).append(item.getType().name()).append(I18n.text("labels.amount")).append(item.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasLore()) result.append("\n\n").append(String.join("\n", meta.getLore()));
            meta.getEnchants().forEach((enchantment, level) -> result.append("\n").append(enchantment.getKey().getKey()).append(" ").append(level));
        }
        return MarketService.stripColors(result.toString());
    }
}
