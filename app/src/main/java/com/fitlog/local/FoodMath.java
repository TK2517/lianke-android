package com.fitlog.local;

public final class FoodMath {
    private FoodMath() {}
    public static double scaled(double per100g, double grams) {
        if (!Double.isFinite(per100g) || !Double.isFinite(grams) || per100g < 0 || grams < 0) {
            throw new IllegalArgumentException("Nutrition values and grams must be finite and nonnegative");
        }
        return per100g * grams / 100.0;
    }
    public static double per100g(double total, double yieldGrams) {
        if (!Double.isFinite(total) || !Double.isFinite(yieldGrams) || total < 0 || yieldGrams <= 0) {
            throw new IllegalArgumentException("Recipe yield must be positive");
        }
        return total * 100.0 / yieldGrams;
    }
}
