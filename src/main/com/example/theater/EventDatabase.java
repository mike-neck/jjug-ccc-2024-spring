package com.example.theater;

import org.jetbrains.annotations.NotNull;

public interface EventDatabase {

  boolean isValidDiscountTicket(@NotNull DiscountTicket discountTicket);
}
