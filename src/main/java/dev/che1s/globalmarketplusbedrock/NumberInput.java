package dev.che1s.globalmarketplusbedrock;

import java.math.BigDecimal;

/** Rejects non-finite values, overflow and fractional quantities. */
final class NumberInput {
    private NumberInput() {}

    static double decimal(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.length() > 64 || !text.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")) {
            throw new NumberFormatException("Invalid decimal");
        }
        BigDecimal exact = new BigDecimal(text);
        double value = exact.doubleValue();
        if (!Double.isFinite(value) || (value == 0 && exact.signum() != 0)) {
            throw new NumberFormatException("Decimal out of range");
        }
        return value;
    }

    static int amount(String raw) {
        decimal(raw);
        try {
            return new BigDecimal(raw.trim()).intValueExact();
        } catch (ArithmeticException ex) {
            throw new NumberFormatException("Quantity must be an integer in range");
        }
    }
}
