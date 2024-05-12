package com.example.theater;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record ShareHolderTicket(@NotNull UUID id, @NotNull UUID owner) implements DiscountType {}
