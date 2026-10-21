package dev.che1s.globalmarketplusbedrock;

import java.util.Locale;
import studio.trc.bukkit.globalmarketplus.merchandise.MerchandiseType;

/** Parses only supported read/menu commands. All mutation commands stay with GMP. */
final class GmpRoute {
    enum Kind { MAIN, MARKET, OWN, MAIL, PROFILE, CATEGORIES, STORES, STORE, DETAIL, EDIT, CONTAINER, LISTING, BULK_SELL, BULK_BUY, SEARCH }
    final Kind kind;
    String target;
    int page;
    long uid;
    MerchandiseType type;
    private GmpRoute(Kind kind) { this.kind = kind; }

    static GmpRoute parse(String command) {
        String[] p = command.trim().split("\\s+");
        if (p.length == 0 || !p[0].equalsIgnoreCase("gmp")) return null;
        if (p.length == 1) return new GmpRoute(Kind.MAIN);
        String group = p[1].toLowerCase(Locale.ROOT), action = p.length > 2 ? p[2].toLowerCase(Locale.ROOT) : "";
        try {
            GmpRoute r;
            if (group.equals("market")) {
                if (action.isEmpty()) return new GmpRoute(Kind.MAIN);
                if (action.equals("gui") && p.length <= 5) {
                    r = new GmpRoute(Kind.MARKET);
                    boolean hasPage = false, hasType = false;
                    for (int i = 3; i < p.length; i++) {
                        if (p[i].matches("[0-9]+")) {
                            if (hasPage) return null;
                            r.page = page(p[i]); hasPage = true;
                        } else {
                            if (hasType) return null;
                            r.type = type(p[i]); hasType = true;
                        }
                    }
                    return r;
                }
                if (p.length == 3) {
                    if (action.equals("search")) return new GmpRoute(Kind.SEARCH);
                    if (action.equals("sellinventory")) return new GmpRoute(Kind.BULK_SELL);
                    if (action.equals("buyinventory")) return new GmpRoute(Kind.BULK_BUY);
                    if (action.equals("sell") || action.equals("buy")) return new GmpRoute(Kind.LISTING);
                }
            }
            if (group.equals("merchandise") && action.equals("list") && p.length <= 5) {
                r = new GmpRoute(Kind.OWN);
                if (p.length > 3) r.page = page(p[3]);
                if (p.length > 4) r.type = type(p[4]);
                return r;
            }
            if ((group.equals("merchandise") && (action.equals("gui") || action.equals("shulkerbox") || action.equals("bundle"))
                    || (group.equals("transaction") || group.equals("auction")) && action.equals("gui")) && p.length == 4) {
                r = new GmpRoute(group.equals("merchandise") ? action.equals("gui") ? Kind.EDIT : Kind.CONTAINER : Kind.DETAIL);
                r.uid = Long.parseLong(p[3]);
                if (r.uid < 0) return null;
                return r;
            }
            if (group.equals("mailbox") && action.equals("gui") && p.length <= 5) {
                r = new GmpRoute(Kind.MAIL);
                boolean hasPage = false;
                for (int i = 3; i < p.length; i++) {
                    if (p[i].matches("[0-9]+")) {
                        if (hasPage) return null;
                        r.page = page(p[i]); hasPage = true;
                    }
                    else if (r.target == null) r.target = p[i];
                    else return null;
                }
                return r;
            }
            if ((group.equals("profile") || group.equals("category")) && action.equals("gui") && p.length <= 4) {
                r = new GmpRoute(group.equals("profile") ? Kind.PROFILE : Kind.CATEGORIES);
                if (p.length == 4) r.target = p[3];
                return r;
            }
            if (group.equals("store") && action.equals("list") && p.length >= 4 && p.length <= 5
                    && p[p.length - 1].equalsIgnoreCase("gui")) {
                r = new GmpRoute(Kind.STORES);
                if (p.length == 5) r.page = page(p[3]);
                return r;
            }
            if (group.equals("store") && action.equals("view") && p.length <= 5) {
                r = new GmpRoute(Kind.STORE);
                if (p.length > 3) r.target = p[3];
                if (p.length > 4) r.page = page(p[4]);
                return r;
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid/unknown parameters must keep GMP's own help and validation.
        }
        return null;
    }
    private static int page(String text) {
        int number = Integer.parseInt(text);
        if (number <= 0) throw new IllegalArgumentException("Invalid page");
        return number - 1;
    }
    private static MerchandiseType type(String text) {
        if (text.equalsIgnoreCase("any")) return null;
        for (MerchandiseType type : new MerchandiseType[] {MerchandiseType.SELLING, MerchandiseType.PURCHASING, MerchandiseType.BIDDING}) {
            if (type.name().equalsIgnoreCase(text)) return type;
        }
        throw new IllegalArgumentException("Unsupported merchandise type");
    }
}
