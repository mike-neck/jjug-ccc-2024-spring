package com.example.theater;

import java.time.LocalDate;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record DiscountTicket(
    @NotNull UUID eventId,
    int ticketSeqId,
    @NotNull LocalDate distributionDate,
    @NotNull Price price)
    implements OptionalDiscount {

  @Override
  public boolean isForAll() {
    return false;
  }
}
