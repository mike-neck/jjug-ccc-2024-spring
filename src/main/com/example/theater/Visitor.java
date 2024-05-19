package com.example.theater;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Visitor(
    @NotNull UUID id,
    @Nullable DiscountType discount,
    @NotNull List<OptionalDiscount> optionalDiscounts,
    @Deprecated @Nullable PersonalStamp personalStamp) {

  public Visitor(
      @NotNull UUID id,
      @Nullable DiscountType discount,
      OptionalDiscount @NotNull ... optionalDiscounts) {
    this(id, discount, List.of(optionalDiscounts), null);
  }

  @Override
  public @Nullable PersonalStamp personalStamp() {
    for (OptionalDiscount optionalDiscount : optionalDiscounts) {
      if (optionalDiscount instanceof PersonalStamp ps) {
        return ps;
      }
    }
    return null;
  }
}
