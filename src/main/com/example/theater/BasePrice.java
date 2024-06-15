package com.example.theater;

import java.util.ArrayList;

record BasePrice(Price price, ArrayList<Discount> discounts) {}
