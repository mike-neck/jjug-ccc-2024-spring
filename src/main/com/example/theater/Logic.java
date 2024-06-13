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
import org.jetbrains.annotations.Unmodifiable;

public class Logic {

  final @NotNull PriceConfiguration priceConfiguration;
  final @NotNull PublishedShareHolderTicketsDatabase publishedShareHolderTicketsDatabase;
  final @NotNull InternetPremiumMembersDatabase internetPremiumMembersDatabase;
  final @NotNull EventDatabase eventDatabase;

  public Logic(
      @NotNull PriceConfiguration priceConfiguration,
      @NotNull PublishedShareHolderTicketsDatabase publishedShareHolderTicketsDatabase,
      @NotNull InternetPremiumMembersDatabase internetPremiumMembersDatabase,
      @NotNull EventDatabase eventDatabase) {
    this.priceConfiguration = priceConfiguration;
    this.publishedShareHolderTicketsDatabase = publishedShareHolderTicketsDatabase;
    this.internetPremiumMembersDatabase = internetPremiumMembersDatabase;
    this.eventDatabase = eventDatabase;
  }

  public @NotNull List<Audience> calculateAdmissionFee(@NotNull VisitorGroup visitorGroup) {
    Price basePrice = priceConfiguration.getBasePrice();
    Map<UUID, Price> visitorToPrice = new HashMap<>();
    Map<UUID, List<Discount>> visitorToDiscount = new HashMap<>();
    boolean companionDiscountAvailable = false;
    Price eightyPercentOfBasePrice = new Price(basePrice.value() * 4 / 5);
    Price twentyPercentOfBasePrice =
        new Price(basePrice.value() - eightyPercentOfBasePrice.value());
    Price halfOfBasePrice = new Price(basePrice.value() / 2);

    for (Visitor visitor : visitorGroup) {
      UUID visitorId = visitor.id();
      DiscountType discountTypeByVisitorProperties = visitor.discount();
      if (discountTypeByVisitorProperties == null) {
        if (companionDiscountAvailable) {
          visitorToPrice.put(visitorId, eightyPercentOfBasePrice);
          visitorToDiscount.put(
              visitorId,
              new ArrayList<>(
                  List.of(
                      new Discount(
                          twentyPercentOfBasePrice,
                          DiscountDescription.of("障がい者割引", DiscountTypes.DISABILITIES)))));
          companionDiscountAvailable = false;
        } else {
          visitorToPrice.put(visitorId, basePrice);
          visitorToDiscount.put(visitorId, new ArrayList<>());
        }
      } else {
        if (discountTypeByVisitorProperties instanceof ShareHolderTicket s) {
          if (publishedShareHolderTicketsDatabase.isPublishedShareHolderTicket(s.id())) {
            for (Visitor companionVisitor : visitorGroup) {
              visitorToPrice.put(companionVisitor.id(), new Price(0));
              visitorToDiscount.put(
                  companionVisitor.id(),
                  List.of(new Discount(basePrice, DiscountDescription.of("株主優待券", s))));
            }
            break;
          } else {
            visitorToPrice.put(visitorId, basePrice);
            visitorToDiscount.put(visitorId, new ArrayList<>());
          }
        } else if (discountTypeByVisitorProperties instanceof DiscountTypes discountType) {
          switch (discountType) {
            case CHILD -> {
              visitorToPrice.put(visitorId, halfOfBasePrice);
              visitorToDiscount.put(
                  visitorId,
                  new ArrayList<>(
                      List.of(
                          new Discount(
                              halfOfBasePrice, DiscountDescription.of("子供割引", discountType)))));
            }
            case DISABILITIES -> {
              visitorToPrice.put(visitorId, eightyPercentOfBasePrice);
              visitorToDiscount.put(
                  visitorId,
                  new ArrayList<>(
                      List.of(
                          new Discount(
                              twentyPercentOfBasePrice,
                              DiscountDescription.of("障がい者割引", discountType)))));
              boolean companionFound = false;
              for (UUID companionVisitorId : Set.copyOf(visitorToPrice.keySet())) {
                if (basePrice.equals(visitorToPrice.get(companionVisitorId))) {
                  companionFound = true;
                  visitorToPrice.put(companionVisitorId, eightyPercentOfBasePrice);
                  visitorToDiscount.put(
                      companionVisitorId,
                      new ArrayList<>(
                          List.of(
                              new Discount(
                                  twentyPercentOfBasePrice,
                                  DiscountDescription.of("障がい者割引", discountType)))));
                  break;
                }
              }
              if (!companionFound) {
                companionDiscountAvailable = true;
              }
            }
            case FEMALES, ELDERLIES -> {
              LocalDate today = priceConfiguration.getToday();
              DayOfWeek todayDayOfWeek = today.getDayOfWeek();
              String discountTitle = discountType == DiscountTypes.FEMALES ? "女性割引" : "シニア割引";
              if (todayDayOfWeek == DayOfWeek.WEDNESDAY
                  && (today.getMonth() != Month.JANUARY || 3 < today.getDayOfMonth())) {
                visitorToPrice.put(visitorId, eightyPercentOfBasePrice);
                visitorToDiscount.put(
                    visitorId,
                    new ArrayList<>(
                        List.of(
                            new Discount(
                                twentyPercentOfBasePrice,
                                DiscountDescription.of(discountTitle, discountType)))));
              } else {
                visitorToPrice.put(visitorId, basePrice);
                visitorToDiscount.put(visitorId, new ArrayList<>());
              }
            }
          }
        }
      }
    }
    applyOptionalDiscounts(visitorGroup, visitorToPrice, visitorToDiscount, halfOfBasePrice);
    return compileAudienceList(
        visitorGroup, new VisitorFeeDetails(visitorToPrice, visitorToDiscount));
  }

  private void applyOptionalDiscounts(
      @NotNull VisitorGroup visitorGroup,
      @NotNull Map<UUID, Price> visitorToPrice,
      @NotNull Map<UUID, List<Discount>> visitorToDiscount,
      @NotNull Price halfOfBasePrice) {
    for (Visitor visitor : visitorGroup) {
      PersonalStamp personalStamp = visitor.personalStamp();
      if (personalStamp != null && personalStamp.count() == 10) {
        Price price = visitorToPrice.get(visitor.id());
        if (price != null && halfOfBasePrice.value() < price.value()) {
          Price discountedPrice = new Price(price.value() - 200);
          boolean fullyDiscounted = halfOfBasePrice.value() < discountedPrice.value();
          visitorToPrice.put(visitor.id(), fullyDiscounted ? discountedPrice : halfOfBasePrice);
          visitorToDiscount
              .computeIfAbsent(visitor.id(), _ -> new ArrayList<>())
              .add(
                  new Discount(
                      fullyDiscounted
                          ? new Price(200)
                          : new Price(price.value() - halfOfBasePrice.value()),
                      DiscountDescription.of("スタンプ割引", personalStamp)));
        }
      }
      for (OptionalDiscount.Offered discountMethod : visitorGroup.allOfferedOptionalDiscounts()) {
        if (!discountMethod.canApplyTo(visitor)) {
          continue;
        }
        OptionalDiscount optionalDiscount = discountMethod.discount();
        List<Discount> discountList =
            visitorToDiscount.computeIfAbsent(visitor.id(), _ -> new ArrayList<>());
        if (discountList.stream()
            .map(u -> u.description().getSource())
            .map(Object::getClass)
            .anyMatch(c -> c.isInstance(optionalDiscount))) {
          continue;
        }
        switch (optionalDiscount) {
          case ShoppingReceipt receipt -> {
            Price currentPrice = visitorToPrice.get(visitor.id());
            if (currentPrice != null
                && halfOfBasePrice.value() < currentPrice.value()
                && 5000 <= receipt.totalPayment()) {
              Price discountedPrice = new Price(currentPrice.value() - 100);
              boolean higherThanHalfOfBasePrice = halfOfBasePrice.value() < discountedPrice.value();
              visitorToPrice.put(
                  visitor.id(), higherThanHalfOfBasePrice ? discountedPrice : halfOfBasePrice);
              discountList.add(
                  new Discount(
                      higherThanHalfOfBasePrice
                          ? new Price(100)
                          : new Price(currentPrice.value() - halfOfBasePrice.value()),
                      DiscountDescription.of("商品購入割引", optionalDiscount)));
            }
          }
          case InternetPremiumMember internetPremiumMember -> {
            Price currentPrice = visitorToPrice.get(visitor.id());
            if (currentPrice != null
                && halfOfBasePrice.value() < currentPrice.value()
                && internetPremiumMembersDatabase.isValidMemberId(internetPremiumMember.userId())) {
              Price discountedPrice = new Price(currentPrice.value() - 200);
              boolean higherThanHalfOfBasePrice = halfOfBasePrice.value() < discountedPrice.value();
              visitorToPrice.put(
                  visitor.id(), higherThanHalfOfBasePrice ? discountedPrice : halfOfBasePrice);
              discountList.add(
                  new Discount(
                      higherThanHalfOfBasePrice
                          ? new Price(200)
                          : new Price(currentPrice.value() - halfOfBasePrice.value()),
                      DiscountDescription.of("インターネットプレミアム会員割引", optionalDiscount)));
            }
          }
          case DiscountTicket discountTicket -> {
            Price currentPrice = visitorToPrice.get(visitor.id());
            if (eventDatabase.isValidDiscountTicket(discountTicket)
                && currentPrice != null
                && halfOfBasePrice.value() < currentPrice.value()
                && discountTicket.price().value() < currentPrice.value()) {
              Price discountedPrice =
                  new Price(currentPrice.value() - discountTicket.price().value());
              boolean higherThanHalfOfBasePrice = halfOfBasePrice.value() < discountedPrice.value();
              visitorToPrice.put(
                  visitor.id(), higherThanHalfOfBasePrice ? discountedPrice : halfOfBasePrice);
              discountList.add(
                  new Discount(
                      higherThanHalfOfBasePrice
                          ? discountTicket.price()
                          : new Price(currentPrice.value() - halfOfBasePrice.value()),
                      DiscountDescription.of("割引チケット", discountTicket)));
            }
          }
          default -> {}
        }
      }
    }
  }

  private static @Unmodifiable List<Audience> compileAudienceList(
      @NotNull VisitorGroup visitorGroup, VisitorFeeDetails visitorFeeDetails) {
    List<Audience> audienceList = new ArrayList<>();
    for (Visitor visitor : visitorGroup) {
      UUID visitorId = visitor.id();
      // FIXME 株主優待券の場合にスタンプを増やさない
      PersonalStamp newPersonalStamp = visitor.nextPersonalStamp();
      Price finalPrice = visitorFeeDetails.getPriceOrThrow(visitorId);
      List<Discount> discountDetails = visitorFeeDetails.getDiscountListOrThrow(visitorId);
      Audience ad = new Audience(visitorId, newPersonalStamp, finalPrice, discountDetails);
      audienceList.add(ad);
    }
    return List.copyOf(audienceList);
  }
}
