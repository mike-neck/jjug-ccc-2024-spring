package com.example.theater;

import org.jetbrains.annotations.NotNull;

public record Discount(@NotNull Price price, @NotNull DiscountDescription description) {}
