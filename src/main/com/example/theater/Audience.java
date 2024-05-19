package com.example.theater;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record Audience(
    @NotNull UUID id,
    @NotNull PersonalStamp newPersonalStamp,
    @NotNull Price price,
    @NotNull List<Discount> discountDetails) {}
