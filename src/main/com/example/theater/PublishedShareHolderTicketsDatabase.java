package com.example.theater;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface PublishedShareHolderTicketsDatabase {

  boolean isPublishedShareHolderTicket(@NotNull UUID ticketId);
}
