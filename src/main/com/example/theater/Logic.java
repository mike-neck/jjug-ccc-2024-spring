package com.example.theater;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class Logic {

  final @NotNull PriceConfiguration priceConfiguration;
  final @NotNull PublishedShareHolderTicketsDatabase publishedShareHolderTicketsDatabase;
  final @NotNull InternetPremiumMembersDatabase internetPremiumMembersDatabase;

  public Logic(
          @NotNull PriceConfiguration priceConfiguration,
          @NotNull PublishedShareHolderTicketsDatabase publishedShareHolderTicketsDatabase,
          @NotNull InternetPremiumMembersDatabase internetPremiumMembersDatabase
  ) {
    this.priceConfiguration = priceConfiguration;
    this.publishedShareHolderTicketsDatabase = publishedShareHolderTicketsDatabase;
    this.internetPremiumMembersDatabase = internetPremiumMembersDatabase;
  }

  public @NotNull List<Audience> calculateAdmissionFee(@NotNull VisitorGroup visitorGroup) {
    Price p = priceConfiguration.getBasePrice();
    Map<UUID, Price> bp = new HashMap<>();
    Map<UUID, List<Discount>> dp = new HashMap<>();
    boolean a = false;
    Price d20 = new Price(p.value() * 4 / 5);
    Price d80 = new Price(p.value() - d20.value());
    Price d50 = new Price(p.value() / 2);

    for (Visitor v : visitorGroup) {
      UUID i = v.id();
      DiscountType d = v.discount();
      if (d == null) {
        if (a) {
          bp.put(i, d20);
          dp.put(i, new ArrayList<>(List.of(new Discount(
                  d80,
                  DiscountDescription.of("障がい者割引", DiscountTypes.DISABILITIES)))
          ));
          a = false;
        } else {
          bp.put(i, p);
          dp.put(i, new ArrayList<>());
        }
      } else {
        if (d instanceof ShareHolderTicket s) {
          if (i.equals(s.owner())
              && publishedShareHolderTicketsDatabase.isPublishedShareHolderTicket(s.id())) {
            for (Visitor g : visitorGroup) {
              bp.put(g.id(), new Price(0));
              dp.put(g.id(), List.of(new Discount(p, DiscountDescription.of("株主優待券", s))));
            }
            List<Audience> l = new ArrayList<>();
            for (Visitor g : visitorGroup) {
              PersonalStamp ps = g.personalStamp();
              List<Discount> ds = dp.computeIfAbsent(g.id(), _ -> new ArrayList<>());
              Audience u = new Audience(g.id(), ps == null ? new PersonalStamp(0) : ps, new Price(0), List.copyOf(ds));
              l.add(u);
            }
            return List.copyOf(l);
          } else {
            bp.put(i, p);
            dp.put(i, new ArrayList<>());
          }
        } else if (d instanceof DiscountTypes t) {
          switch (t) {
            case CHILD -> {
              bp.put(i, d50);
              dp.put(i, new ArrayList<>(List.of(new Discount(d50, DiscountDescription.of("子供割引", t)))));
            }
            case DISABILITIES -> {
              bp.put(i, d20);
              dp.put(i, new ArrayList<>(List.of(new Discount(d80, DiscountDescription.of("障がい者割引", t)))));
              boolean f = false;
              for (UUID u : Set.copyOf(bp.keySet())) {
                if (p.equals(bp.get(u))) {
                  f = true;
                  bp.put(u, d20);
                  dp.put(u, new ArrayList<>(List.of(new Discount(d80, DiscountDescription.of("障がい者割引", t)))));
                  break;
                }
              }
              if (!f) {
                a = true;
              }
            }
            case SENIOR_CITIZENS -> {
              bp.put(i, d20);
              dp.put(i, new ArrayList<>(List.of(new Discount(d80, DiscountDescription.of("シルバー割引", t)))));
            }
            case FEMALES -> {
              LocalDate l = priceConfiguration.getToday();
              DayOfWeek w = l.getDayOfWeek();
              if (w == DayOfWeek.WEDNESDAY && (l.getMonth() != Month.JANUARY || l.getDayOfMonth() <= 3)) {
                bp.put(i, d20);
                dp.put(i, new ArrayList<>(List.of(new Discount(d80, DiscountDescription.of("女性割引", t)))));
              }
            }
          }
        }
      }
    }
    for (Visitor v : visitorGroup) {
      PersonalStamp s = v.personalStamp();
      if (s != null && s.count() == 10) {
        Price c = bp.get(v.id());
        if (c != null && d50.value() < c.value()) {
          Price n = new Price(c.value() - 200);
          boolean b = d50.value() < n.value();
          bp.put(v.id(), b ? n: d50);
          dp.computeIfAbsent(v.id(), _ -> new ArrayList<>()).add(new Discount(
                  b? new Price(200): new Price(c.value() - d50.value()),
                  DiscountDescription.of("スタンプ割引", s)
          ));
        }
      }
      for (OptionalDiscount o : visitorGroup.optionalDiscounts()) {
        switch (o) {
          case ShoppingReceipt r -> {
            Price c = bp.get(v.id());
            if (c != null && d50.value() < c.value() && 5000 <= r.totalPayment()) {
              Price n = new Price(c.value() - 100);
              boolean b = d50.value() < n.value();
              bp.put(v.id(), b ? n: d50);
              dp.computeIfAbsent(v.id(), _ -> new ArrayList<>()).add(new Discount(
                      b? new Price(100): new Price(c.value() - d50.value()),
                      DiscountDescription.of("商品購入割引", o)
              ));
            }
          }
          case InternetPremiumMember i -> {
            Price c = bp.get(v.id());
            if (c != null && d50.value() < c.value() && internetPremiumMembersDatabase.isValidMemberId(i.userId())) {
              Price n = new Price(c.value() - 100);
              boolean b = d50.value() < n.value();
              bp.put(v.id(), b ? n: d50);
              dp.computeIfAbsent(v.id(), _ -> new ArrayList<>()).add(new Discount(
                      b? new Price(100): new Price(c.value() - d50.value()),
                      DiscountDescription.of("インターネットプレミアム会員割引", o)
              ));
            }
          }
          case DiscountTicket t -> {
            Price c = bp.get(v.id());
            if (c != null && d50.value() < c.value() && t.price().value() < c.value()) {
                Price n = new Price(c.value() - t.price().value());
                boolean b = d50.value() < n.value();
                bp.put(v.id(), b ? n : d50);
                dp.computeIfAbsent(v.id(), _ -> new ArrayList<>()).add(new Discount(
                        b ? t.price() : new Price(c.value() - d50.value()),
                        DiscountDescription.of("割引チケット", t)
                ));
            }
          }
          default -> {}
        }
      }
    }
    List<Audience> l = new ArrayList<>();
    for (Visitor v : visitorGroup) {
      UUID id = v.id();
      PersonalStamp s = v.personalStamp();
      PersonalStamp n =
          s == null || s.count() == 10 ?
            new PersonalStamp(1) :
            new PersonalStamp(s.count() + 1);
      Price c = bp.get(id);
      if (c == null) {
        throw new IllegalStateException("error");
      }
      List<Discount> d = dp.get(id);
      if (d == null) {
        throw new IllegalStateException("error");
      }
      Audience ad = new Audience(id, n, c, d);
      l.add(ad);
    }
    return List.copyOf(l);
  }
}
