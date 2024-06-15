package com.example.theater;

record SelectedPrice(
    BasePrice basePrice, VisitorFeeDetails feeDetailsForShareHolder, boolean companionDiscount) {}
