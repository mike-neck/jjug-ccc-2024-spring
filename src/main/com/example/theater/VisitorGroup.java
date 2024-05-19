package com.example.theater;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Represents a group of visitors.
 *
 * @param visitors - all visitor in this group.
 */
public record VisitorGroup(@NotNull Collection<@NotNull Visitor> visitors)
    implements Iterable<@NotNull Visitor> {

  public @NotNull @Unmodifiable Collection<OptionalDiscount> allOptionalDiscounts() {
    List<OptionalDiscount> discounts = new ArrayList<>();
    for (Visitor visitor : visitors) {
      discounts.addAll(visitor.optionalDiscounts());
    }
    return List.copyOf(discounts);
  }

  public @NotNull @Unmodifiable Collection<OptionalDiscount.Offered> allOfferedOptionalDiscounts() {
    List<OptionalDiscount.Offered> discounts = new ArrayList<>();
    for (Visitor visitor : visitors) {
      for (OptionalDiscount discount : visitor.optionalDiscounts()) {
        discounts.add(new OptionalDiscount.Offered(visitor, discount));
      }
    }
    return List.copyOf(discounts);
  }

  @NotNull
  @Override
  public Iterator<@NotNull Visitor> iterator() {
    return visitors.iterator();
  }
}
