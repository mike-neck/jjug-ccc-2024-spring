package com.example.theater;

public record PersonalStamp(int count) implements OptionalDiscount {
  @Override
  public boolean isForAll() {
    return false;
  }
}
