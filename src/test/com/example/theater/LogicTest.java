package com.example.theater;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

class LogicTest {

  static @NotNull final LocalDate NEW_YEAR_WEDNESDAY = LocalDate.of(2020, 1, 1);
  static @NotNull final LocalDate NEW_YEAR_NONE_WEDNESDAY = LocalDate.of(2020, 1, 3);
  static @NotNull final LocalDate NONE_NEW_YEAR_WEDNESDAY = LocalDate.of(2023, 1, 4);
  static @NotNull final LocalDate NONE_NEW_YEAR_NONE_WEDNESDAY = LocalDate.of(2023, 1, 5);

  static @NotNull Visitor shareHolder(
      @NotNull UUID visitorId, int stampCount, OptionalDiscount @NotNull ... discounts) {
    PersonalStamp personalStamp = stampCount < 0 ? null : new PersonalStamp(stampCount);
    OptionalDiscount[] optionalDiscounts =
        new OptionalDiscount[(personalStamp == null ? 0 : 1) + discounts.length];
    int index = 0;
    if (personalStamp != null) {
      optionalDiscounts[index++] = personalStamp;
    }
    System.arraycopy(discounts, 0, optionalDiscounts, index, discounts.length);
    return new Visitor(
        visitorId, new ShareHolderTicket(UUID.randomUUID(), UUID.randomUUID()), optionalDiscounts);
  }

  static @NotNull Visitor normalVisitor(int stampCount) {
    if (stampCount == -1) {

      return new Visitor(UUID.randomUUID(), null);
    }
    return new Visitor(UUID.randomUUID(), null, new PersonalStamp(stampCount));
  }

  static @NotNull Visitor normalVisitor(
      @NotNull UUID visitorId, int stampCount, OptionalDiscount @NotNull ... optionalDiscounts) {
    OptionalDiscount[] optionalDiscountArray = new OptionalDiscount[1 + optionalDiscounts.length];
    optionalDiscountArray[0] = new PersonalStamp(stampCount);
    System.arraycopy(optionalDiscounts, 0, optionalDiscountArray, 1, optionalDiscounts.length);
    return new Visitor(visitorId, null, optionalDiscountArray);
  }

  static @NotNull Visitor child(int stampCount) {
    return child(UUID.randomUUID(), stampCount);
  }

  static @NotNull Visitor child(@NotNull UUID visitorId, int stampCount) {
    return visitor(visitorId, stampCount, DiscountType.CHILD);
  }

  static @NotNull Visitor disability(
      @NotNull UUID visitorId, int stampCount, OptionalDiscount @NotNull ... discounts) {
    return visitor(visitorId, stampCount, DiscountType.DISABILITIES, discounts);
  }

  static @NotNull Visitor elderly(@NotNull UUID visitorId, int stampCount) {
    DiscountType type = DiscountType.SENIOR_CITIZENS;
    return visitor(visitorId, stampCount, type);
  }

  static @NotNull Visitor visitor(
      @NotNull UUID visitorId,
      int stampCount,
      @NotNull DiscountType type,
      OptionalDiscount @NotNull ... discounts) {
    PersonalStamp personalStamp = stampCount < 0 ? null : new PersonalStamp(stampCount);
    OptionalDiscount[] discountArray =
        new OptionalDiscount[(personalStamp == null ? 0 : 1) + discounts.length];
    int index = 0;
    if (personalStamp != null) {
      discountArray[index++] = personalStamp;
    }
    System.arraycopy(discounts, 0, discountArray, index, discounts.length);
    return new Visitor(visitorId, type, discountArray);
  }

  @Test
  void singleShareHolder() {
    UUID visitor = UUID.randomUUID();
    VisitorGroup visitors = new VisitorGroup(List.of(shareHolder(visitor, 0)));
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(new Price(1000), LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    assertAll(
        () -> assertEquals(1, audiences.size()),
        () -> assertEquals(visitor, audiences.getFirst().id()),
        () -> assertEquals(new Price(0), audiences.getFirst().price()),
        () -> assertEquals(1, audiences.getFirst().discountDetails().size()));
  }

  @Test
  void singleShareHolderWithAnotherVisitors() {
    UUID visitor = UUID.randomUUID();
    VisitorGroup visitors =
        new VisitorGroup(List.of(normalVisitor(0), shareHolder(visitor, 1), normalVisitor(5)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    var zero = new Price(0);
    assertAll(
        () -> assertEquals(3, audiences.size()),
        () ->
            assertEquals(
                List.of(zero, zero, zero), audiences.stream().map(Audience::price).toList()),
        () ->
            assertEquals(
                List.of(basePrice, basePrice, basePrice),
                audiences.stream()
                    .flatMap(a -> a.discountDetails().stream())
                    .map(Discount::price)
                    .toList()),
        () ->
            assertEquals(
                Set.of(new PersonalStamp(0), new PersonalStamp(1), new PersonalStamp(5)),
                audiences.stream().map(Audience::newPersonalStamp).collect(Collectors.toSet())));
  }

  @Test
  void multipleShareHolders() {
    UUID visitor1 = UUID.randomUUID();
    UUID visitor2 = UUID.randomUUID();
    VisitorGroup visitors =
        new VisitorGroup(
            List.of(shareHolder(visitor1, 0), normalVisitor(-1), shareHolder(visitor2, 0)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    var zero = new Price(0);
    assertAll(
        () -> assertEquals(3, audiences.size()),
        () ->
            assertEquals(
                List.of(zero, zero, zero), audiences.stream().map(Audience::price).toList()),
        () ->
            assertEquals(
                List.of(basePrice, basePrice, basePrice),
                audiences.stream()
                    .flatMap(a -> a.discountDetails().stream())
                    .map(Discount::price)
                    .toList()));
  }

  @Test
  void singleNormalVisitor() {
    UUID visitor = UUID.randomUUID();
    VisitorGroup visitors = new VisitorGroup(List.of(normalVisitor(visitor, -1)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    assertAll(
        () -> assertEquals(1, audiences.size()),
        () -> assertEquals(visitor, audiences.getFirst().id()),
        () -> assertEquals(basePrice, audiences.getFirst().price()),
        () -> assertTrue(audiences.getFirst().discountDetails().isEmpty()));
  }

  @Test
  void multipleNormalVisitors() {
    UUID visitor1 = UUID.randomUUID();
    UUID visitor2 = UUID.randomUUID();
    VisitorGroup visitors =
        new VisitorGroup(List.of(normalVisitor(visitor1, -1), normalVisitor(visitor2, 3)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    assertAll(
        () -> assertEquals(2, audiences.size()),
        () ->
            assertEquals(
                List.of(basePrice, basePrice), audiences.stream().map(Audience::price).toList()),
        () -> assertTrue(audiences.stream().allMatch(a -> a.discountDetails().isEmpty())));
  }

  @Test
  void child() {
    UUID visitor = UUID.randomUUID();
    VisitorGroup visitors = new VisitorGroup(List.of(child(visitor, -1)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    Price halfPrice = new Price(500);
    assertAll(
        () -> assertEquals(1, audiences.size()),
        () -> assertEquals(visitor, audiences.getFirst().id()),
        () -> assertEquals(halfPrice, audiences.getFirst().price()),
        () -> assertEquals(halfPrice, audiences.getFirst().discountDetails().getFirst().price()));
  }

  @Test
  void singleChildWithNormal() {
    UUID child = UUID.randomUUID();
    UUID normal = UUID.randomUUID();
    VisitorGroup visitors = new VisitorGroup(List.of(child(child, -1), normalVisitor(normal, 3)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    Price halfPrice = new Price(500);
    assertAll(
        () -> assertEquals(2, audiences.size()),
        () ->
            assertEquals(
                Set.of(child, normal),
                audiences.stream().map(Audience::id).collect(Collectors.toSet())),
        () ->
            assertEquals(
                List.of(halfPrice, basePrice), audiences.stream().map(Audience::price).toList()),
        () ->
            assertEquals(
                List.of(halfPrice),
                audiences.stream()
                    .filter(a -> !a.discountDetails().isEmpty())
                    .map(a -> a.discountDetails().getFirst().price())
                    .toList()));
  }

  @Test
  void singleDisability() {
    UUID visitor = UUID.randomUUID();
    VisitorGroup visitors = new VisitorGroup(List.of(disability(visitor, -1)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    Price discountedPrice = new Price(800);
    Price discount = new Price(200);
    assertAll(
        () -> assertEquals(1, audiences.size()),
        () -> assertEquals(visitor, audiences.getFirst().id()),
        () -> assertEquals(discountedPrice, audiences.getFirst().price()),
        () -> assertEquals(discount, audiences.getFirst().discountDetails().getFirst().price()));
  }

  @Test
  void singleDisabilityWithNormal() {
    UUID disability = UUID.randomUUID();
    UUID normal = UUID.randomUUID();
    VisitorGroup visitors =
        new VisitorGroup(List.of(disability(disability, -1), normalVisitor(normal, 3)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    Price discountedPrice = new Price(800);
    Price discount = new Price(200);
    assertAll(
        () -> assertEquals(2, audiences.size()),
        () ->
            assertEquals(
                Set.of(disability, normal),
                audiences.stream().map(Audience::id).collect(Collectors.toSet())),
        () ->
            assertEquals(
                List.of(discountedPrice, discountedPrice),
                audiences.stream().map(Audience::price).toList()),
        () ->
            assertEquals(
                List.of(discount, discount),
                audiences.stream()
                    .filter(a -> !a.discountDetails().isEmpty())
                    .map(a -> a.discountDetails().getFirst().price())
                    .toList()));
  }

  @Test
  void singleNormalWithSingleDisability() {
    UUID disability = UUID.randomUUID();
    UUID normal = UUID.randomUUID();
    VisitorGroup visitors =
        new VisitorGroup(List.of(normalVisitor(normal, 2), disability(disability, 3)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    Price discountedPrice = new Price(800);
    Price discount = new Price(200);
    assertAll(
        () -> assertEquals(2, audiences.size()),
        () ->
            assertEquals(
                Set.of(disability, normal),
                audiences.stream().map(Audience::id).collect(Collectors.toSet())),
        () ->
            assertEquals(
                List.of(discountedPrice, discountedPrice),
                audiences.stream().map(Audience::price).toList()),
        () ->
            assertEquals(
                List.of(discount, discount),
                audiences.stream()
                    .filter(a -> !a.discountDetails().isEmpty())
                    .map(a -> a.discountDetails().getFirst().price())
                    .toList()));
  }

  @Test
  void singleDisabilityWithMultipleNormal() {
    UUID disability = UUID.randomUUID();
    UUID normal = UUID.randomUUID();
    UUID normal2 = UUID.randomUUID();
    VisitorGroup visitors =
        new VisitorGroup(
            List.of(
                normalVisitor(normal, 2), disability(disability, 3), normalVisitor(normal2, -1)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    Price discountedPrice = new Price(800);
    Price discount = new Price(200);
    assertAll(
        () -> assertEquals(3, audiences.size()),
        () ->
            assertEquals(
                Set.of(disability, normal, normal2),
                audiences.stream().map(Audience::id).collect(Collectors.toSet())),
        () ->
            assertEquals(
                List.of(discountedPrice, discountedPrice, basePrice),
                audiences.stream().map(Audience::price).toList()),
        () ->
            assertEquals(
                List.of(discount, discount),
                audiences.stream()
                    .filter(a -> !a.discountDetails().isEmpty())
                    .map(a -> a.discountDetails().getFirst().price())
                    .toList()));
  }

  @Test
  void singleDisabilityWithChild() {
    UUID disability = UUID.randomUUID();
    UUID child = UUID.randomUUID();
    VisitorGroup visitors = new VisitorGroup(List.of(child(child, 2), disability(disability, 3)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);
    Price disabilityPrice = new Price(800);
    Price disabilityDiscount = new Price(200);
    Price childPrice = new Price(500);
    Price childDiscount = new Price(500);
    assertAll(
        () -> assertEquals(2, audiences.size()),
        () ->
            assertEquals(
                Set.of(disability, child),
                audiences.stream().map(Audience::id).collect(Collectors.toSet())),
        () ->
            assertEquals(
                List.of(childPrice, disabilityPrice),
                audiences.stream().map(Audience::price).toList()),
        () ->
            assertEquals(
                List.of(childDiscount, disabilityDiscount),
                audiences.stream()
                    .filter(a -> !a.discountDetails().isEmpty())
                    .map(a -> a.discountDetails().getFirst().price())
                    .toList()));
  }

  @TestFactory
  Stream<DynamicTest> singleDiscountVisitor() {
    Price fullPrice = new Price(1000);
    Price discountedPrice = new Price(800);
    Price discount = new Price(200);
    return Stream.of(DiscountTypes.FEMALES, DiscountTypes.ELDERLIES)
        .flatMap(
            type ->
                Stream.of(
                    toDynamicTest(
                        new DiscountByDateTest(
                            "No new year + No Wednesday -> Not Discount",
                            type,
                            NONE_NEW_YEAR_NONE_WEDNESDAY,
                            fullPrice,
                            null)),
                    toDynamicTest(
                        new DiscountByDateTest(
                            "No new year + Wednesday -> Discount",
                            type,
                            NONE_NEW_YEAR_WEDNESDAY,
                            discountedPrice,
                            discount)),
                    toDynamicTest(
                        new DiscountByDateTest(
                            "New year + No Wednesday -> Not Discount",
                            type,
                            NEW_YEAR_NONE_WEDNESDAY,
                            fullPrice,
                            null)),
                    toDynamicTest(
                        new DiscountByDateTest(
                            "New year + Wednesday -> Not Discount",
                            type,
                            NEW_YEAR_WEDNESDAY,
                            fullPrice,
                            null))));
  }

  private static @NotNull DynamicTest toDynamicTest(DiscountByDateTest test) {
    return dynamicTest(test.name(), test);
  }

  record DiscountByDateTest(
      String testName,
      @NotNull DiscountTypes type,
      @NotNull LocalDate date,
      @NotNull Price discountedPrice,
      @Nullable Price discount)
      implements Executable {

    @NotNull
    String name() {
      //noinspection preview
      return STR."[\{type}] \{testName}";
    }

    @Override
    public void execute() {
      @Nullable Price discount = discount();
      UUID elderly = UUID.randomUUID();
      VisitorGroup visitors = new VisitorGroup(List.of(visitor(elderly, -1, type)));
      Price basePrice = new Price(1000);
      Logic logic =
          new Logic(
              new FixedPriceConfiguration(basePrice, date()), _ -> true, _ -> false, _ -> false);
      List<Audience> audiences = logic.calculateAdmissionFee(visitors);
      Executable discountPriceTest =
          discount == null
              ? () -> assertTrue(audiences.getFirst().discountDetails().isEmpty(), testName)
              : () ->
                  assertEquals(
                      discount,
                      audiences.getFirst().discountDetails().getFirst().price(),
                      testName);
      assertAll(
          () -> assertEquals(1, audiences.size(), testName),
          () -> assertEquals(elderly, audiences.getFirst().id(), testName),
          () -> assertEquals(discountedPrice(), audiences.getFirst().price(), testName),
          discountPriceTest);
    }
  }

  @Test
  void stampDiscount() {
    UUID visitor = UUID.randomUUID();
    VisitorGroup visitors = new VisitorGroup(List.of(normalVisitor(visitor, 10)));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, NONE_NEW_YEAR_NONE_WEDNESDAY),
            _ -> true,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);

    assertAll(
        () -> assertEquals(1, audiences.size()),
        () -> assertEquals(visitor, audiences.getFirst().id()),
        () -> assertEquals(new Price(800), audiences.getFirst().price()),
        () -> assertEquals(1, audiences.getFirst().discountDetails().size()),
        () ->
            assertEquals(new Price(200), audiences.getFirst().discountDetails().getFirst().price()),
        () -> assertEquals(new PersonalStamp(1), audiences.getFirst().newPersonalStamp()));
  }

  @TestFactory
  Stream<DynamicTest> eachDiscountTypes() {
    record InputAndExpected(
        @NotNull String name,
        @NotNull Price expectedDiscountedPrice,
        @NotNull Price expectedDiscount,
        @NotNull VisitorGroup input) {
      @Contract(pure = true)
      @NotNull
      Executable createTest(@NotNull UUID visitor, @NotNull Logic logic) {
        return () -> {
          List<Audience> audiences = logic.calculateAdmissionFee(input());
          assertAll(
              () -> assertEquals(1, audiences.size()),
              () -> assertEquals(visitor, audiences.getFirst().id()),
              () -> assertEquals(expectedDiscountedPrice(), audiences.getFirst().price()),
              () -> assertEquals(1, audiences.getFirst().discountDetails().size()),
              () ->
                  assertEquals(
                      expectedDiscount(),
                      audiences.getFirst().discountDetails().getFirst().price()));
        };
      }
    }

    UUID visitor = UUID.randomUUID();
    UUID internetPremium = UUID.randomUUID();
    List<InputAndExpected> visitorGroups =
        List.of(
            new InputAndExpected(
                "スタンプ割引は200円",
                new Price(800),
                new Price(200),
                new VisitorGroup(List.of(normalVisitor(visitor, 10)))),
            new InputAndExpected(
                "商品購入割引は100円",
                new Price(900),
                new Price(100),
                new VisitorGroup(List.of(normalVisitor(visitor, 3, new ShoppingReceipt(5001))))),
            new InputAndExpected(
                "インターネットプレミアム割引は200円",
                new Price(800),
                new Price(200),
                new VisitorGroup(
                    List.of(
                        normalVisitor(visitor, 3, new InternetPremiumMember(internetPremium))))),
            new InputAndExpected(
                "割引券割引は券面記載額(400円)",
                new Price(600),
                new Price(400),
                new VisitorGroup(
                    List.of(
                        normalVisitor(
                            visitor,
                            3,
                            new DiscountTicket(
                                UUID.randomUUID(), 1, NEW_YEAR_WEDNESDAY, new Price(400)))))));

    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, NONE_NEW_YEAR_NONE_WEDNESDAY),
            _ -> true,
            internetPremium::equals,
            t -> NONE_NEW_YEAR_NONE_WEDNESDAY.isAfter(t.distributionDate()));

    return visitorGroups.stream()
        .map(iae -> dynamicTest(iae.name(), iae.createTest(visitor, logic)));
  }

  @Test
  void discountToBeHalfOfBasePrice() {
    UUID visitor = UUID.randomUUID();
    Price basePrice = new Price(1000);
    VisitorGroup visitors =
        new VisitorGroup(
            List.of(
                disability(
                    visitor,
                    10,
                    new ShoppingReceipt(5001),
                    new InternetPremiumMember(UUID.randomUUID()),
                    new DiscountTicket(UUID.randomUUID(), 2, LocalDate.now(), new Price(400)))));
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, NONE_NEW_YEAR_NONE_WEDNESDAY),
            _ -> true,
            _ -> true,
            _ -> true);

    List<Audience> audiences = logic.calculateAdmissionFee(visitors);

    assertAll(
        () -> assertEquals(1, audiences.size()),
        () -> assertEquals(visitor, audiences.getFirst().id()),
        () -> assertEquals(new Price(500), audiences.getFirst().price()),
        () -> assertEquals(3, audiences.getFirst().discountDetails().size()),
        () ->
            assertEquals(
                Set.of(new Price(200), new Price(100)),
                audiences.getFirst().discountDetails().stream()
                    .map(Discount::price)
                    .collect(Collectors.toSet())));
  }

  @Test
  void allOptionalDiscountsWillNotBeAppliedToShareHolders() {
    UUID visitor = UUID.randomUUID();
    Price basePrice = new Price(1000);
    VisitorGroup visitors =
        new VisitorGroup(
            List.of(
                shareHolder(
                    visitor,
                    0,
                    new ShoppingReceipt(5001),
                    new InternetPremiumMember(UUID.randomUUID()),
                    new DiscountTicket(UUID.randomUUID(), 2, LocalDate.now(), new Price(400)))));
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, NONE_NEW_YEAR_NONE_WEDNESDAY),
            _ -> true,
            _ -> true,
            _ -> true);

    List<Audience> audiences = logic.calculateAdmissionFee(visitors);

    assertAll(
        () -> assertEquals(1, audiences.size()),
        () -> assertEquals(visitor, audiences.getFirst().id()),
        () -> assertEquals(new Price(0), audiences.getFirst().price()),
        () -> assertEquals(1, audiences.getFirst().discountDetails().size()));
  }

  @TestFactory
  Stream<DynamicTest> invalidDiscountWillNotBeApplied() {
    record TestCase(@NotNull String name, @NotNull VisitorGroup visitors) {}
    UUID visitor = UUID.randomUUID();
    Price basePrice = new Price(1000);
    List<TestCase> visitorGroups =
        List.of(
            new TestCase("不正株主優待券で割引は適用されない", new VisitorGroup(List.of(shareHolder(visitor, 1)))),
            new TestCase(
                "不正会員には割引は適用されない",
                new VisitorGroup(
                    List.of(
                        normalVisitor(visitor, 1, new InternetPremiumMember(UUID.randomUUID()))))),
            new TestCase(
                "不正な割引券は適用されない",
                new VisitorGroup(
                    List.of(
                        normalVisitor(
                            visitor,
                            1,
                            new DiscountTicket(
                                UUID.randomUUID(), 10, NEW_YEAR_WEDNESDAY, new Price(300)))))));
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, NONE_NEW_YEAR_NONE_WEDNESDAY),
            _ -> false,
            _ -> false,
            _ -> false);

    return visitorGroups.stream()
        .map(
            testCase ->
                dynamicTest(
                    testCase.name(),
                    () -> {
                      List<Audience> audiences = logic.calculateAdmissionFee(testCase.visitors());
                      assertAll(
                          () -> assertTrue(audiences.getFirst().discountDetails().isEmpty()),
                          () -> assertEquals(basePrice, audiences.getFirst().price()));
                    }));
  }

  @TestFactory
  Stream<DynamicTest> optionalGroupDiscountWillApplyToHolderAndCompanion() {
    record TestCase(
        @NotNull String name, @NotNull Price expectedPrice, @NotNull VisitorGroup visitors) {}
    UUID visitor = UUID.randomUUID();
    UUID companion = UUID.randomUUID();
    List<TestCase> visitorGroups =
        List.of(
            new TestCase(
                "商品購入割引は全員に適用される",
                new Price(900),
                new VisitorGroup(
                    List.of(
                        normalVisitor(visitor, 1),
                        normalVisitor(companion, 1, new ShoppingReceipt(5001))))),
            new TestCase(
                "プレミアム会員割引は全員に適用される",
                new Price(800),
                new VisitorGroup(
                    List.of(
                        normalVisitor(visitor, 1, new InternetPremiumMember(UUID.randomUUID())),
                        normalVisitor(companion, 1)))));
    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> true,
            _ -> false);

    return visitorGroups.stream()
        .map(
            testCase ->
                dynamicTest(
                    testCase.name(),
                    () -> {
                      List<Audience> audiences = logic.calculateAdmissionFee(testCase.visitors());
                      assertAll(
                          () -> assertEquals(2, audiences.size()),
                          () ->
                              assertEquals(
                                  List.of(testCase.expectedPrice(), testCase.expectedPrice()),
                                  audiences.stream().map(Audience::price).toList()),
                          () ->
                              assertTrue(
                                  audiences.stream()
                                      .noneMatch(
                                          audience -> audience.discountDetails().isEmpty())));
                    }));
  }

  @TestFactory
  Stream<DynamicTest> optionalSingleDiscountWillApplyOnlyThoseWhoPresentIt() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    record TestCase(
        @NotNull String name,
        @NotNull Map<UUID, Price> expectedPrice,
        @NotNull VisitorGroup visitors) {
      void assertAudiencePaysExpectedPrice(@NotNull String targetName, @NotNull Audience audience) {
        Price expected = expectedPrice.get(audience.id());
        assertEquals(
            expected,
            audience.price(),
            () ->
                "%s: expected=%s actual=%s id=%s %s"
                    .formatted(
                        targetName,
                        expected,
                        audience.price(),
                        audience.id(),
                        TestCase.this.expectedPrice));
      }
    }

    Price basePrice = new Price(1000);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, LocalDate.of(2020, 2, 3)),
            _ -> true,
            _ -> true,
            _ -> true);

    List<TestCase> testCases =
        List.of(
            new TestCase(
                "スタンプ割引は提示した者のみ適用される",
                Map.ofEntries(Map.entry(first, new Price(800)), Map.entry(second, basePrice)),
                new VisitorGroup(List.of(normalVisitor(first, 10), normalVisitor(second, 1)))),
            new TestCase(
                "割引券は提示した者のみ適用される",
                Map.ofEntries(Map.entry(first, basePrice), Map.entry(second, new Price(800))),
                new VisitorGroup(
                    List.of(
                        normalVisitor(first, 1),
                        normalVisitor(
                            second,
                            1,
                            new DiscountTicket(
                                UUID.randomUUID(), 20, NEW_YEAR_WEDNESDAY, new Price(200)))))));

    return testCases.stream()
        .map(
            testCase ->
                dynamicTest(
                    testCase.name(),
                    () -> {
                      List<Audience> audiences = logic.calculateAdmissionFee(testCase.visitors());
                      assertAll(
                          () -> assertEquals(2, audiences.size()),
                          () ->
                              testCase.assertAudiencePaysExpectedPrice("1st", audiences.getFirst()),
                          () ->
                              testCase.assertAudiencePaysExpectedPrice("2nd", audiences.getLast()));
                    }));
  }

  @Test
  void multipleSameTypeDiscountWillNotApply() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    VisitorGroup visitors =
        new VisitorGroup(
            List.of(
                normalVisitor(first, 2, new ShoppingReceipt(5001)),
                normalVisitor(second, 1, new ShoppingReceipt(12501))));
    Price basePrice = new Price(300);
    Logic logic =
        new Logic(
            new FixedPriceConfiguration(basePrice, NONE_NEW_YEAR_NONE_WEDNESDAY),
            _ -> false,
            _ -> false,
            _ -> false);
    List<Audience> audiences = logic.calculateAdmissionFee(visitors);

    assertAll(
        () -> assertEquals(2, audiences.size()),
        () -> {
          Audience fst = audiences.getFirst();
          assertAll(
              () -> assertEquals(new Price(200), fst.price()),
              () -> assertEquals(1, fst.discountDetails().size()));
        },
        () -> {
          Audience last = audiences.getLast();
          assertAll(
              () -> assertEquals(new Price(200), last.price()),
              () -> assertEquals(1, last.discountDetails().size()));
        });
  }
}

record FixedPriceConfiguration(@Override Price getBasePrice, @Override LocalDate getToday)
    implements PriceConfiguration {}
