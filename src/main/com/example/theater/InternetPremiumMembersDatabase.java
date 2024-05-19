package com.example.theater;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface InternetPremiumMembersDatabase {
  boolean isValidMemberId(@NotNull UUID memberId);
}
