package dev.che1s.globalmarketplusbedrock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.configuration.file.YamlConfiguration;
import studio.trc.bukkit.globalmarketplus.api.APIUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import studio.trc.bukkit.globalmarketplus.api.Merchant;
import studio.trc.bukkit.globalmarketplus.merchandise.Merchandise;
import studio.trc.bukkit.globalmarketplus.merchandise.MerchandiseSort;
import studio.trc.bukkit.globalmarketplus.tool.Category;

/** Short lived, per-player browsing snapshots. All actions look up live UIDs again. */
final class MarketCatalog {
    private final long ttl;
    private final AtomicLong generation = new AtomicLong();
    private final SnapshotCache<List<Merchandise>> market;
    private final Map<UUID, View> views = new ConcurrentHashMap<>();
    private static final class View {
        final BrowseQuery query;
        final String category, sort;
        final long until, generation;
        final List<Merchandise> items;
        View(BrowseQuery query, String category, String sort, long until, long generation, List<Merchandise> items) {
            this.query = query; this.category = category; this.sort = sort; this.until = until; this.generation = generation; this.items = items;
        }
    }
    MarketCatalog(GlobalMarketPlusBedrock plugin) {
        ttl = Math.max(0, Math.min(5000L, plugin.getConfig().getLong("display-cache-millis", 1000L)));
        market = new SnapshotCache<>(ttl, System::currentTimeMillis);
    }
    void invalidate() { generation.incrementAndGet(); market.invalidate(); views.clear(); }
    void forget(Player player) { views.remove(player.getUniqueId()); }

    List<Merchandise> list(Player player, BrowseQuery query) {
        long version = generation.get();
        Merchant viewer = MarketService.getMerchant(player);
        Category category = viewer.getProfile().getCategory();
        MerchandiseSort sort = viewer.getProfile().getMerchandiseSort();
        if (sort == null) sort = MerchandiseSort.getDefaultSort();
        String categoryKey = category == null ? "" : category.getName();
        String sortKey = sort.getName();
        View cached = views.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (cached != null && cached.generation == version && now < cached.until && query.equals(cached.query)
                && categoryKey.equals(cached.category) && sortKey.equals(cached.sort)) return cached.items;
        List<Merchandise> source = market.get(MarketService::all);
        Map<UUID, Merchant> owners = new HashMap<>();
        Map<String, Boolean> currencies = new HashMap<>();
        YamlConfiguration permissions = MarketService.require(APIUtils.getConfig("OtherPermissions.yml"), I18n.text("errors.currency-config"));
        Category filterCategory = null;
        for (Category candidate : Category.getCategories()) {
            if (candidate.getName().equalsIgnoreCase(query.filter.category)) { filterCategory = candidate; break; }
        }
        if (!query.filter.category.isEmpty() && (filterCategory == null || !filterCategory.hasPermission(player))) {
            throw new MarketException(I18n.text("errors.search-category"));
        }
        List<Merchandise> result = new ArrayList<>();
        for (Merchandise item : source) {
            if (item == null || item.isExpired() || (query.type != null && item.getMerchandiseType() != query.type)) continue;
            if (query.owner != null && !query.owner.equals(item.getOwnerUUID())) continue;
            Merchant owner = owners.computeIfAbsent(item.getOwnerUUID(), MarketService::merchant);
            if (!query.own && !player.getUniqueId().equals(item.getOwnerUUID()) && !MarketService.publicListing(item, owner)) continue;
            if (category != null && !category.inCategory(item)) continue;
            if (filterCategory != null && !filterCategory.inCategory(item)) continue;
            String currency = MarketService.currency(item);
            if (!currencies.computeIfAbsent(currency, key -> GmpAccess.rule(player, permissions, "Currencies." + key))) continue;
            SearchFilter f = query.filter;
            double price = item.isAllowWholesale() ? item.getPrice() : item.getRetailPrice();
            if (!f.priceMatches(price)) continue;
            ItemStack stack = item.getItem();
            String seller = (query.owner != null || query.own) ? item.getOwnerName() : MarketService.getSellerDisplay(item, owner);
            ItemMeta meta = (!f.name.isEmpty() || !f.lore.isEmpty()) && stack.hasItemMeta() ? stack.getItemMeta() : null;
            String name = f.name.isEmpty() ? "" : MarketService.getItemName(stack);
            String lore = meta != null && meta.hasLore() ? MarketService.stripColors(String.join("\n", meta.getLore())).replace('\n', ' ') : "";
            if (!f.textMatches(stack.getType().name(), seller, name, lore, MarketService.currency(item))) continue;
            result.add(item);
        }
        if ("RANDOM".equals(sortKey)) Collections.shuffle(result);
        else {
            final MerchandiseSort selected = sort;
            result.sort((a, b) -> { int order = a.compareTo(b, selected); return order == 0 ? Long.compare(b.getMerchandiseUID(), a.getMerchandiseUID()) : order; });
        }
        List<Merchandise> snapshot = Collections.unmodifiableList(result);
        views.put(player.getUniqueId(), new View(query, categoryKey, sortKey, now + ttl, version, snapshot));
        return snapshot;
    }
}
