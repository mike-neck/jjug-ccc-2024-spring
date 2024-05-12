package com.example.theater;

import org.jetbrains.annotations.NotNull;

public interface DiscountDescription {
    @NotNull String getText();
    @NotNull Object getSource();

    static @NotNull DiscountDescription of(@NotNull String text, @NotNull Object source) {
        return new DiscountDescription() {
            @Override
            public @NotNull String getText() {
                return text;
            }

            @Override
            public @NotNull Object getSource() {
                return source;
            }
        };
    }
}
