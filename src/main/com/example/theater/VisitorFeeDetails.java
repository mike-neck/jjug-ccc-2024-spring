package com.example.theater;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record VisitorFeeDetails(
    Map<UUID, Price> visitorToPrice, Map<UUID, List<Discount>> visitorToDiscount) {}
