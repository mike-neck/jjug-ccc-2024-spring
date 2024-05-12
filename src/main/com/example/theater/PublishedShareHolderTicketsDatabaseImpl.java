package com.example.theater;

import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class PublishedShareHolderTicketsDatabaseImpl implements PublishedShareHolderTicketsDatabase {

  final @NotNull Set<@NotNull UUID> publishedTickets =
      Set.of(
          UUID.fromString("4A8E3117-57FA-4B03-B823-934933319D94"),
          UUID.fromString("BF7B5929-46F9-484D-A5D2-CAE6E23E1FE9"),
          UUID.fromString("7648285F-A001-4745-A7D3-53A880BF4320"),
          UUID.fromString("DAA9B485-19D5-4BCA-A375-9889CFE5E33D"),
          UUID.fromString("DB5F1BE9-13FF-4877-9B55-D41580BD12CC"),
          UUID.fromString("5022F21E-75C1-44AA-93D7-5EC48F5213E5"),
          UUID.fromString("7A696A9D-3294-4159-90BB-2DCE93FA5F1C"),
          UUID.fromString("5B491058-9893-4039-A158-94140067281E"),
          UUID.fromString("2BD3230A-EEE2-4DB5-B114-2BB126A47897"),
          UUID.fromString("1CD539FA-D21F-4369-90A7-8EF5F808F922")
      );

  @Override
  public boolean isPublishedShareHolderTicket(@NotNull UUID ticketId) {
    return publishedTickets.contains(ticketId);
  }
}
