package com.example.theater;

import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Visitor(
        @NotNull UUID id,
        @Nullable DiscountType discount,
        @Nullable PersonalStamp personalStamp
) {}
