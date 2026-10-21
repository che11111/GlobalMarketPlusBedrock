package dev.che1s.globalmarketplusbedrock;

import studio.trc.bukkit.globalmarketplus.merchandise.Merchandise;
import studio.trc.bukkit.globalmarketplus.merchandise.MerchandiseType;

/** Immutable displayed terms: a live Merchandise object can change during confirmation. */
final class TransactionQuote {
    final double price;
    private final String currency;
    private final MerchandiseType type;
    private final Integer amount;
    private final int stock;

    TransactionQuote(Merchandise item, Integer amount) {
        this(amount == null ? item.getPrice() : item.getRetailPrice(), MarketService.currency(item),
            item.getMerchandiseType(), amount, item.getItem().getAmount());
    }

    TransactionQuote(double price, String currency, MerchandiseType type, Integer amount, int stock) {
        if (!Double.isFinite(price) || price < 0) throw new MarketException(I18n.text("errors.invalid-quote"));
        this.price = price; this.currency = currency; this.type = type; this.amount = amount; this.stock = stock;
    }

    void validate(Merchandise item) {
        validate(amount == null ? item.getPrice() : item.getRetailPrice(), MarketService.currency(item),
            item.getMerchandiseType(), item.getItem().getAmount(),
            amount == null ? item.isAllowWholesale() : item.isAllowRetail());
    }

    void validate(double price, String currency, MerchandiseType type, int stock, boolean allowed) {
        if (!allowed || Double.compare(this.price, price) != 0 || this.type != type
                || !this.currency.equals(currency) || (amount == null && this.stock != stock)) {
            throw new MarketException(I18n.text("errors.quote-changed"));
        }
    }
}
