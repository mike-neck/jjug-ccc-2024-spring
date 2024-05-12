package com.example.theater;

import java.util.Collection;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;

public record VisitorGroup(
        @NotNull Collection<@NotNull Visitor> visitors,
        @NotNull Collection<OptionalDiscount> optionalDiscounts
) implements Iterable<@NotNull Visitor> {
    @NotNull
    @Override
    public Iterator<@NotNull Visitor> iterator() {
        return visitors.iterator();
    }
}
