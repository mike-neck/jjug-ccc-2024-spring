package com.example.theater;

public sealed interface DiscountType permits ShareHolderTicket, DiscountTypes {
  DiscountType CHILD = DiscountTypes.CHILD;
  DiscountType DISABILITIES = DiscountTypes.DISABILITIES;
  DiscountType SENIOR_CITIZENS = DiscountTypes.ELDERLIES;
  DiscountType FEMALES = DiscountTypes.FEMALES;
}
