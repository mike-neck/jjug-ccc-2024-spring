package com.example.theater;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record VisitorFeeDetails(
    Map<UUID, Price> visitorToPrice, Map<UUID, List<Discount>> visitorToDiscount) {

  @NotNull
  Price getPriceOrThrow(@NotNull UUID visitorId) {
    Price finalPrice = visitorToPrice().get(visitorId);
    if (finalPrice == null) {
      //noinspection preview
      throw new IllegalStateException(
          STR."error \{visitorId} not found in \{visitorToPrice().keySet()}");
    }
    return finalPrice;
  }
}
