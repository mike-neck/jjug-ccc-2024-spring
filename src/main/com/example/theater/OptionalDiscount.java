package com.example.theater;

import org.jetbrains.annotations.NotNull;

public sealed interface OptionalDiscount
    permits DiscountTicket, InternetPremiumMember, PersonalStamp, ShoppingReceipt {

  default boolean isForAll() {
    return true;
  }

  record Offered(@NotNull Visitor visitor, @NotNull OptionalDiscount discount) {
    public boolean canApplyTo(@NotNull Visitor visitor) {
      return discount.isForAll() || this.visitor.equals(visitor);
    }
  }
}
