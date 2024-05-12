package com.example.theater;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record InternetPremiumMember(@NotNull UUID userId) implements OptionalDiscount {}
