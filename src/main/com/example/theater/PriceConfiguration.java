package com.example.theater;

import java.time.LocalDate;

public interface PriceConfiguration {
    Price getBasePrice();
    LocalDate getToday();
}
