package dev.che1s.globalmarketplusbedrock;

/** Semantic roles keep icons independent of translated or administrator-edited labels. */
enum ButtonKind {
    MARKET("$", "items/emerald"), PURCHASE("+", "items/gold_ingot"),
    AUCTION("★", "items/gold_axe"), OWN("◆", "items/diamond"),
    MAIL("✉", "items/book_written"), STORE("⌂", "blocks/chest_front"),
    LISTING("↑", "items/hopper"), SEARCH("?", "items/compass_item"),
    ALL("≡", "items/book_normal"), PROFILE("✎", "items/name_tag"),
    CATEGORY("▤", "items/book_normal"), SORT("↕", "items/comparator"),
    REFRESH("↻", "items/clock_item"), BACK("←", "items/arrow"),
    PREVIOUS("«", "items/arrow"), NEXT("»", "items/arrow"),
    CONFIRM("✓", "items/emerald"), CANCEL("×", "blocks/barrier"),
    DELETE("×", "blocks/barrier"), EDIT("✎", "items/feather"),
    OPTIONS("⚙", "items/redstone_dust"), CONTAINER("▣", "items/shulker_shell"),
    COLLECT("↓", "items/minecart_chest"), ADD("+", "items/emerald"),
    SUBTRACT("−", "items/redstone_dust");

    final String symbol;
    final String path;
    ButtonKind(String symbol, String path) { this.symbol = symbol; this.path = "textures/" + path; }
}
