package dev.che1s.globalmarketplusbedrock;

import java.util.Locale;
import java.util.Objects;

/** Literal, case-insensitive filters; no untrusted regular expressions. */
final class SearchFilter {
    static final SearchFilter EMPTY = new SearchFilter("", "", "", "", "", "", "", "");
    final String item, seller, name, lore, currency, category;
    final Double min, max;

    SearchFilter(String item, String seller, String name, String lore, String min, String max, String currency, String category) {
        this.item = normalize(item).replace("minecraft:", "");
        this.seller = normalize(seller);
        this.name = normalize(name);
        this.lore = normalize(lore);
        this.currency = normalize(currency);
        this.category = normalize(category);
        this.min = number(min);
        this.max = number(max);
        if (this.min != null && this.max != null && this.min > this.max) throw new IllegalArgumentException("Invalid price range");
    }

    private static Double number(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        double value = NumberInput.decimal(raw);
        if (value < 0) throw new IllegalArgumentException("Negative price");
        return value;
    }
    private static String normalize(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (text.length() > 256 || text.contains("\n") || text.contains("\r")) throw new IllegalArgumentException("Invalid filter");
        return text;
    }
    boolean textMatches(String material, String seller, String name, String lore, String currency) {
        return lower(material).contains(item) && lower(seller).contains(this.seller)
            && lower(name).contains(this.name) && lower(lore).contains(this.lore)
            && (this.currency.isEmpty() || lower(currency).equals(this.currency));
    }
    private static String lower(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
    boolean priceMatches(double price) { return (min == null || price >= min) && (max == null || price <= max); }
    @Override public boolean equals(Object value) {
        if (!(value instanceof SearchFilter)) return false;
        SearchFilter f = (SearchFilter) value;
        return item.equals(f.item) && seller.equals(f.seller) && name.equals(f.name) && lore.equals(f.lore)
            && currency.equals(f.currency) && category.equals(f.category) && Objects.equals(min, f.min) && Objects.equals(max, f.max);
    }
    @Override public int hashCode() { return Objects.hash(item, seller, name, lore, currency, category, min, max); }
}
