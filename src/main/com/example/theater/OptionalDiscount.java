package com.example.theater;

public sealed interface OptionalDiscount
        permits DiscountTicket, InternetPremiumMember, PersonalStamp, ShoppingReceipt
{}
