package com.jirapat.prpo.api.dto.request;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;


@DisplayName("PurchaseRequestItemRequest — validation")
class PurchaseRequestItemValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static PurchaseRequestItemRequest.PurchaseRequestItemRequestBuilder validItem() {
        return PurchaseRequestItemRequest.builder()
                .description("Valid description")
                .quantity(BigDecimal.ONE)
                .unit("pcs")
                .estimatedPrice(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("quantity ติดลบ ถูกปฏิเสธ")
    void negativeQuantity_isRejected() {
        PurchaseRequestItemRequest item = validItem()
                .quantity(BigDecimal.valueOf(-100))
                .build();

        Set<ConstraintViolation<PurchaseRequestItemRequest>> violations = validator.validate(item);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("quantity");
    }

    @Test
    @DisplayName("quantity = 0 ถูกปฏิเสธ (@Positive ไม่ยอมรับศูนย์)")
    void zeroQuantity_isRejected() {
        PurchaseRequestItemRequest item = validItem()
                .quantity(BigDecimal.ZERO)
                .build();

        Set<ConstraintViolation<PurchaseRequestItemRequest>> violations = validator.validate(item);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("quantity");
    }

    @Test
    @DisplayName("estimatedPrice ติดลบ ถูกปฏิเสธ")
    void negativeEstimatedPrice_isRejected() {
        PurchaseRequestItemRequest item = validItem()
                .estimatedPrice(BigDecimal.valueOf(-9999))
                .build();

        Set<ConstraintViolation<PurchaseRequestItemRequest>> violations = validator.validate(item);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("estimatedPrice");
    }

    @Test
    @DisplayName("quantity ที่จำนวนหลักเกินขนาด column ถูกปฏิเสธ")
    void oversizedQuantity_isRejected() {
        PurchaseRequestItemRequest item = validItem()
                .quantity(new BigDecimal("99999999999999"))
                .build();

        Set<ConstraintViolation<PurchaseRequestItemRequest>> violations = validator.validate(item);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("quantity");
    }

    @Test
    @DisplayName("estimatedPrice ที่จำนวนหลักเกินขนาด column ถูกปฏิเสธ")
    void oversizedEstimatedPrice_isRejected() {
        PurchaseRequestItemRequest item = validItem()
                .estimatedPrice(new BigDecimal("99999999999999"))
                .build();

        Set<ConstraintViolation<PurchaseRequestItemRequest>> violations = validator.validate(item);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("estimatedPrice");
    }

    @Test
    @DisplayName("estimatedPrice = null ยังผ่านได้ (เป็น field ที่ไม่บังคับ)")
    void nullEstimatedPrice_passesValidation() {
        PurchaseRequestItemRequest item = validItem()
                .estimatedPrice(null)
                .build();

        Set<ConstraintViolation<PurchaseRequestItemRequest>> violations = validator.validate(item);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("item ที่ค่าถูกต้องครบ ผ่าน validation")
    void validItem_passesValidation() {
        Set<ConstraintViolation<PurchaseRequestItemRequest>> violations =
                validator.validate(validItem().build());

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("description ว่าง ถูกปฏิเสธ")
    void blankDescription_isRejected() {
        PurchaseRequestItemRequest item = validItem()
                .description("  ")
                .build();

        Set<ConstraintViolation<PurchaseRequestItemRequest>> violations = validator.validate(item);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("description");
    }
}
