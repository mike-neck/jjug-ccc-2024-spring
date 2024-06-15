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
import org.jetbrains.annotations.Nullable;
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
    VisitorFeeDetails visitorFeeDetails = calculateBasePrices(visitorGroup);
    applyOptionalDiscounts(visitorGroup, visitorFeeDetails);
    return compileAudienceList(visitorGroup, visitorFeeDetails);
  }

  private static @NotNull VisitorFeeDetails createVisitorFeeDetails() {
    Map<UUID, Price> visitorToPrice = new HashMap<>();
    Map<UUID, List<Discount>> visitorToDiscount = new HashMap<>();
    return new VisitorFeeDetails(visitorToPrice, visitorToDiscount);
  }

  private @NotNull VisitorFeeDetails calculateBasePrices(@NotNull VisitorGroup visitorGroup) {
    VisitorFeeDetails visitorFeeDetails = createVisitorFeeDetails();

    boolean companionDiscountAvailable = false;
    for (Visitor visitor : visitorGroup) {
      UUID visitorId = visitor.id();
      DiscountType discountTypeByVisitorProperties = visitor.discount();
      boolean companionDiscount = companionDiscountAvailable;
      SelectedPrice selectedPrice =
          selectPriceByVisitorProperties(
              visitorGroup, discountTypeByVisitorProperties, companionDiscount, visitorFeeDetails);
      if (selectedPrice.feeDetailsForShareHolder() != null) {
        return selectedPrice.feeDetailsForShareHolder();
      }
      if (selectedPrice.basePrice() != null) {
        visitorFeeDetails.setBasePrice(visitorId, selectedPrice.basePrice());
      } else {
        visitorFeeDetails.setBasePrice(
            visitorId, defaultBasePrice(priceConfiguration.getBasePrice()));
      }
      companionDiscountAvailable = selectedPrice.companionDiscount();
    }
    return visitorFeeDetails;
  }

  private @NotNull SelectedPrice selectPriceByVisitorProperties(
      @NotNull VisitorGroup visitorGroup,
      DiscountType discountTypeByVisitorProperties,
      boolean companionDiscount,
      VisitorFeeDetails visitorFeeDetails) {
    Price fullPrice = priceConfiguration.getBasePrice();
    Price eightyPercentOfBasePrice = new Price(fullPrice.value() * 4 / 5);
    Price twentyPercentOfBasePrice =
        new Price(fullPrice.value() - eightyPercentOfBasePrice.value());
    Price halfOfBasePrice = new Price(fullPrice.value() / 2);
    @Nullable BasePrice basePrice = null;
    @Nullable VisitorFeeDetails feeDetailsForShareHolder = null;
    if (discountTypeByVisitorProperties == null) {
      if (companionDiscount) {
        basePrice =
            new BasePrice(
                eightyPercentOfBasePrice,
                new ArrayList<>(
                    List.of(
                        new Discount(
                            twentyPercentOfBasePrice,
                            DiscountDescription.of("障がい者割引", DiscountTypes.DISABILITIES)))));
        companionDiscount = false;
      }
    } else {
      if (discountTypeByVisitorProperties instanceof ShareHolderTicket shareHolderTicket) {
        if (publishedShareHolderTicketsDatabase.isPublishedShareHolderTicket(
            shareHolderTicket.id())) {
          feeDetailsForShareHolder = new VisitorFeeDetails();
          for (Visitor companionVisitor : visitorGroup) {
            Price price = new Price(0);
            BasePrice bp =
                new BasePrice(
                    price,
                    List.of(
                        new Discount(
                            fullPrice, DiscountDescription.of("株主優待券", shareHolderTicket))));
            feeDetailsForShareHolder.setBasePrice(companionVisitor.id(), bp);
          }
          basePrice = null;
          companionDiscount = companionDiscount;
        }
      } else if (discountTypeByVisitorProperties instanceof DiscountTypes discountType) {
        switch (discountType) {
          case CHILD -> {
            basePrice =
                new BasePrice(
                    halfOfBasePrice,
                    new ArrayList<>(
                        List.of(
                            new Discount(
                                halfOfBasePrice, DiscountDescription.of("子供割引", discountType)))));
            companionDiscount = companionDiscount;
          }
          case DISABILITIES -> {
            ArrayList<Discount> discounts =
                new ArrayList<>(
                    List.of(
                        new Discount(
                            twentyPercentOfBasePrice,
                            DiscountDescription.of("障がい者割引", discountType))));
            basePrice = new BasePrice(eightyPercentOfBasePrice, discounts);

            boolean companionFound = false;
            for (UUID companionVisitorId :
                Set.copyOf(visitorFeeDetails.visitorToPrice().keySet())) {
              if (fullPrice.equals(visitorFeeDetails.visitorToPrice().get(companionVisitorId))) {
                companionFound = true;
                visitorFeeDetails
                    .visitorToPrice()
                    .put(companionVisitorId, eightyPercentOfBasePrice);
                visitorFeeDetails.visitorToDiscount().put(companionVisitorId, discounts);
                break;
              }
            }
            if (!companionFound) {
              companionDiscount = true;
            } else {
              companionDiscount = companionDiscount;
            }
          }
          case FEMALES, ELDERLIES -> {
            LocalDate today = priceConfiguration.getToday();
            DayOfWeek todayDayOfWeek = today.getDayOfWeek();
            String discountTitle = discountType == DiscountTypes.FEMALES ? "女性割引" : "シニア割引";
            if (todayDayOfWeek == DayOfWeek.WEDNESDAY
                && (today.getMonth() != Month.JANUARY || 3 < today.getDayOfMonth())) {
              basePrice =
                  new BasePrice(
                      eightyPercentOfBasePrice,
                      new ArrayList<>(
                          List.of(
                              new Discount(
                                  twentyPercentOfBasePrice,
                                  DiscountDescription.of(discountTitle, discountType)))));
              companionDiscount = companionDiscount;
            }
          }
        }
      }
    }
    SelectedPrice selectedPrice =
        new SelectedPrice(basePrice, feeDetailsForShareHolder, companionDiscount);
    return selectedPrice;
  }

  private record SelectedPrice(
      BasePrice basePrice, VisitorFeeDetails feeDetailsForShareHolder, boolean companionDiscount) {}

  private static @NotNull BasePrice defaultBasePrice(Price basePrice) {
    return new BasePrice(basePrice, new ArrayList<>());
  }

  private void applyOptionalDiscounts(
      @NotNull VisitorGroup visitorGroup, @NotNull VisitorFeeDetails visitorFeeDetails) {
    Map<UUID, Price> visitorToPrice = visitorFeeDetails.visitorToPrice();
    Map<UUID, List<Discount>> visitorToDiscount = visitorFeeDetails.visitorToDiscount();
    Price basePrice = priceConfiguration.getBasePrice();
    Price halfOfBasePrice = new Price(basePrice.value() / 2);
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

  private static @NotNull @Unmodifiable List<Audience> compileAudienceList(
      @NotNull VisitorGroup visitorGroup, VisitorFeeDetails visitorFeeDetails) {
    List<Audience> audienceList = new ArrayList<>();
    for (Visitor visitor : visitorGroup) {
      UUID visitorId = visitor.id();
      Price finalPrice = visitorFeeDetails.getPriceOrThrow(visitorId);
      List<Discount> discountDetails = visitorFeeDetails.getDiscountListOrThrow(visitorId);
      PersonalStamp newPersonalStamp = personalStamp(discountDetails, visitor);
      Audience ad = new Audience(visitorId, newPersonalStamp, finalPrice, discountDetails);
      audienceList.add(ad);
    }
    return List.copyOf(audienceList);
  }

  static @NotNull PersonalStamp personalStamp(
      @NotNull List<Discount> discountDetails, @NotNull Visitor visitor) {
    if (discountDetails.stream()
        .map(Discount::description)
        .map(DiscountDescription::getSource)
        .map(Object::getClass)
        .anyMatch(ShareHolderTicket.class::equals)) {
      PersonalStamp personalStamp = visitor.personalStamp();
      return personalStamp == null ? new PersonalStamp(0) : personalStamp;
    } else {
      return visitor.nextPersonalStamp();
    }
  }
}
