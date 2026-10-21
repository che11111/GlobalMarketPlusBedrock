package dev.che1s.globalmarketplusbedrock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PageWindow<T> {
    final int page;
    final int pages;
    final List<T> items;

    private PageWindow(int page, int pages, List<T> items) {
        this.page = page;
        this.pages = pages;
        this.items = Collections.unmodifiableList(items);
    }

    static <T> PageWindow<T> of(List<T> source, int requested, int size) {
        int actualSize = Math.max(1, Math.min(100, size));
        int pages = Math.max(1, (int) ((source.size() + (long) actualSize - 1) / actualSize));
        int page = Math.max(0, Math.min(requested, pages - 1));
        int start = page * actualSize;
        return new PageWindow<>(page, pages,
            new ArrayList<>(source.subList(start, Math.min(source.size(), start + actualSize))));
    }
}
