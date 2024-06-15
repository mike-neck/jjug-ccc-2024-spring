package com.example.theater;

import java.util.List;

record BasePrice(Price price, List<Discount> discounts) {}
