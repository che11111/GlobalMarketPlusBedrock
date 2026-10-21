package dev.che1s.globalmarketplusbedrock;

import java.util.Objects;
import java.util.UUID;
import studio.trc.bukkit.globalmarketplus.merchandise.MerchandiseType;

final class BrowseQuery {
    final MerchandiseType type;
    final UUID owner;
    final boolean own;
    final SearchFilter filter;
    BrowseQuery(MerchandiseType type, UUID owner, boolean own, SearchFilter filter) {
        this.type = type;
        this.owner = owner;
        this.own = own;
        this.filter = filter;
    }
    @Override public boolean equals(Object value) {
        if (!(value instanceof BrowseQuery)) return false;
        BrowseQuery q = (BrowseQuery) value;
        return type == q.type && Objects.equals(owner, q.owner) && own == q.own && filter.equals(q.filter);
    }
    @Override public int hashCode() { return Objects.hash(type, owner, own, filter); }
}
