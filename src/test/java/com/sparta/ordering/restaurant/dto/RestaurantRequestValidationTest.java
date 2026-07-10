package com.sparta.ordering.restaurant.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Digits;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("음식점 요청 DTO 검증")
class RestaurantRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Nested
    @DisplayName("음식점 생성 요청")
    class CreateRequest {

        @Test
        @DisplayName("문자열 최대 길이와 숫자 경계값을 지킨 요청은 허용한다")
        void acceptsValidBoundaryValues() {
            RestaurantCreateRequest request = new RestaurantCreateRequest(
                    "가".repeat(20),
                    "나".repeat(100),
                    "031-1234-5678",
                    "설".repeat(1000),
                    "주".repeat(255),
                    "상".repeat(255),
                    "1".repeat(10),
                    0,
                    0,
                    new BigDecimal("-90.0000000"),
                    new BigDecimal("180.0000000"),
                    new BigDecimal("0.1")
            );

            assertThat(validator.validate(request)).isEmpty();
        }

        @ParameterizedTest(name = "[{index}] {0}={1}")
        @MethodSource("requiredStringValues")
        @DisplayName("필수 문자열 필드에 null과 공백을 입력할 수 없다")
        void rejectsNullAndBlankRequiredStrings(String property, String value) {
            RestaurantCreateRequest request = createRequestWithString(property, value);

            assertViolatesOnlyProperty(request, property);
        }

        /**
         * 필수 문자열 필드의 속성과 해당 필드가 null 또는 공백인 경우를 나타내는 {@link Arguments} 스트림을 반환한다.
         * <p>
         * 이 스트림은 "category", "name", "phone", "address", "addressDetail", "zipCode" 필드를 대상으로 하며,
         * 각 필드에 대해 null과 공백 값을 조합하여 생성된 {@link Arguments} 객체를 포함한다.
         *
         * @return 필수 문자열 필드와 해당 필드에 대한 유효하지 않은 값({@code null} 또는 공백)을 포함한 {@link Arguments} 스트림을 반환한다.
         *
         * <pre>{@code
         * ("category", null)
         * ("category", " ")
         * ("name", null)
         * ("name", " ")
         * ...
         * }</pre>
         */
        static Stream<Arguments> requiredStringValues() {
            return Stream.of("category", "name", "phone", "address", "addressDetail", "zipCode")
                    .flatMap(property -> Stream.of(null, " ")
                            .map(value -> Arguments.of(property, value)));
        }

        @ParameterizedTest(name = "[{index}] {0} 길이={1}")
        @CsvSource({
                "category, 21",
                "name, 101",
                "description, 1001",
                "address, 256",
                "addressDetail, 256",
                "zipCode, 11"
        })
        @DisplayName("문자열 필드의 최대 길이를 초과할 수 없다")
        void rejectsStringsOverMaximumLength(String property, int length) {
            RestaurantCreateRequest request = createRequestWithString(property, "가".repeat(length));

            assertViolatesOnlyProperty(request, property);
        }

        @ParameterizedTest(name = "[{index}] {0}=null")
        @ValueSource(strings = {
                "minOrderAmount",
                "deliveryFee",
                "latitude",
                "longitude",
                "deliveryRadiusKm"
        })
        @DisplayName("필수 숫자 필드는 null을 입력할 수 없다")
        void rejectsNullRequiredNumbers(String property) {
            RestaurantCreateRequest request = createRequestWithNullNumber(property);

            assertViolatesOnlyProperty(request, property);
        }

        @Test
        @DisplayName("전화번호 형식이 올바르지 않으면 실패한다")
        void rejectsMalformedPhone() {
            RestaurantCreateRequest request = createRequestWithString("phone", "01012345678");

            assertViolatesOnlyProperty(request, "phone");
        }

        @ParameterizedTest(name = "[{index}] {0}=-1")
        @ValueSource(strings = {"minOrderAmount", "deliveryFee"})
        @DisplayName("최소 주문 금액과 배달료는 음수일 수 없다.")
        void rejectsNegativeAmounts(String property) {
            RestaurantCreateRequest request = createRequestWithInteger(property, -1);

            assertViolatesOnlyProperty(request, property);
        }

        @Test
        @DisplayName("배달 반경이 0.1km 미만이면 실패한다")
        void rejectsDeliveryRadiusBelowMinimum() {
            RestaurantCreateRequest request = createRequestWithDecimal(
                    "deliveryRadiusKm",
                    new BigDecimal("0.0")
            );

            assertViolatesOnlyProperty(request, "deliveryRadiusKm");
        }

        @ParameterizedTest(name = "[{index}] {0}={1}")
        @CsvSource({
                "latitude, -90.0000001",
                "latitude, 90.0000001",
                "longitude, -180.0000001",
                "longitude, 180.0000001"
        })
        @DisplayName("위도와 경도의 허용 범위를 벗어난 값은 실패한다")
        void rejectsCoordinatesOutsideRange(String property, String value) {
            RestaurantCreateRequest request = createRequestWithDecimal(property, new BigDecimal(value));

            assertViolatesOnlyProperty(request, property);
        }

        @ParameterizedTest(name = "[{index}] {0}={1}")
        @CsvSource({
                "latitude, 1000",
                "latitude, 0.12345678",
                "longitude, 1000",
                "longitude, 0.12345678",
                "deliveryRadiusKm, 1000.0",
                "deliveryRadiusKm, 1.23"
        })
        @DisplayName("좌표와 배달 반경의 정수부 또는 소수부 자릿수를 초과할 수 없다.")
        void rejectsDigitExcess(String property, String value) {
            RestaurantCreateRequest request = createRequestWithDecimal(property, new BigDecimal(value));

            assertDigitsViolation(request, property);
        }

        @ParameterizedTest(name = "[{index}] {0}={1}")
        @CsvSource({
                "latitude, -90.0",
                "latitude, 90.0",
                "longitude, -180.0",
                "longitude, 180.0"
        })
        @DisplayName("위도와 경도의 정확한 최솟값과 최댓값을 허용한다")
        void acceptsExactCoordinateBoundaries(String property, String value) {
            RestaurantCreateRequest request = createRequestWithDecimal(property, new BigDecimal(value));

            assertThat(validator.validate(request)).isEmpty();
        }
    }

    @Nested
    @DisplayName("음식점 수정 요청")
    class UpdateRequest {

        @Test
        @DisplayName("모든 필드가 null인 부분 수정 요청은 허용한다")
        void acceptsAllNullRequest() {
            assertThat(validator.validate(emptyUpdateRequest())).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 description 수정 요청은 빈 문자열로 수정한다")
        void acceptsEmptyDescription() {
            RestaurantUpdateRequest request = updateRequestWithString("description", "");

            assertThat(validator.validate(request)).isEmpty();
        }

        @ParameterizedTest(name = "[{index}] {0}=공백")
        @ValueSource(strings = {"name", "address", "addressDetail", "zipCode"})
        @DisplayName("이름과 주소 관련 필드는 공백을 거부한다")
        void rejectsBlankRequiredContent(String property) {
            RestaurantUpdateRequest request = updateRequestWithString(property, " ");

            assertViolatesOnlyProperty(request, property);
        }

        @ParameterizedTest(name = "[{index}] {0} 길이={1}")
        @CsvSource({
                "category, 21",
                "name, 101",
                "description, 1001",
                "address, 256",
                "addressDetail, 256",
                "zipCode, 11"
        })
        @DisplayName("전달된 문자열 필드의 최대 길이를 초과하면 거부한다")
        void rejectsStringsOverMaximumLength(String property, int length) {
            RestaurantUpdateRequest request = updateRequestWithString(property, "가".repeat(length));

            assertViolatesOnlyProperty(request, property);
        }

        @Test
        @DisplayName("전화번호 형식이 올바르지 않으면 거부한다")
        void rejectsMalformedPhone() {
            RestaurantUpdateRequest request = updateRequestWithString("phone", "01012345678");

            assertViolatesOnlyProperty(request, "phone");
        }

        @ParameterizedTest(name = "[{index}] {0}=-1")
        @ValueSource(strings = {"minOrderAmount", "deliveryFee"})
        @DisplayName("최소 주문 금액과 배달료는 음수를 거부한다")
        void rejectsNegativeAmounts(String property) {
            RestaurantUpdateRequest request = updateRequestWithInteger(property, -1);

            assertViolatesOnlyProperty(request, property);
        }

        @Test
        @DisplayName("배달 반경이 0.1km 미만이면 거부한다")
        void rejectsDeliveryRadiusBelowMinimum() {
            RestaurantUpdateRequest request = updateRequestWithDecimal(
                    "deliveryRadiusKm",
                    new BigDecimal("0.0")
            );

            assertViolatesOnlyProperty(request, "deliveryRadiusKm");
        }

        @ParameterizedTest(name = "[{index}] {0}={1}")
        @CsvSource({
                "latitude, -90.0000001",
                "latitude, 90.0000001",
                "longitude, -180.0000001",
                "longitude, 180.0000001"
        })
        @DisplayName("위도와 경도의 허용 범위를 벗어난 값은 거부한다")
        void rejectsCoordinatesOutsideRange(String property, String value) {
            RestaurantUpdateRequest request = updateRequestWithDecimal(property, new BigDecimal(value));

            assertViolatesOnlyProperty(request, property);
        }

        @ParameterizedTest(name = "[{index}] {0}={1}")
        @CsvSource({
                "latitude, 0.12345678",
                "longitude, 0.12345678",
                "deliveryRadiusKm, 1.23"
        })
        @DisplayName("좌표와 배달 반경의 소수 자릿수 초과를 거부한다")
        void rejectsFractionExcess(String property, String value) {
            RestaurantUpdateRequest request = updateRequestWithDecimal(property, new BigDecimal(value));

            assertDigitsViolation(request, property);
        }
    }

    private static RestaurantCreateRequest validCreateRequest() {
        return new RestaurantCreateRequest(
                "KOREAN",
                "한식당",
                "02-1234-5678",
                "음식점 설명",
                "서울시 강남구",
                "2층",
                "12345",
                15000,
                3000,
                new BigDecimal("37.1234567"),
                new BigDecimal("127.1234567"),
                new BigDecimal("3.5")
        );
    }

    private static RestaurantCreateRequest createRequestWithString(String property, String value) {
        RestaurantCreateRequest valid = validCreateRequest();
        return new RestaurantCreateRequest(
                "category".equals(property) ? value : valid.category(),
                "name".equals(property) ? value : valid.name(),
                "phone".equals(property) ? value : valid.phone(),
                "description".equals(property) ? value : valid.description(),
                "address".equals(property) ? value : valid.address(),
                "addressDetail".equals(property) ? value : valid.addressDetail(),
                "zipCode".equals(property) ? value : valid.zipCode(),
                valid.minOrderAmount(),
                valid.deliveryFee(),
                valid.latitude(),
                valid.longitude(),
                valid.deliveryRadiusKm()
        );
    }

    private static RestaurantCreateRequest createRequestWithNullNumber(String property) {
        RestaurantCreateRequest valid = validCreateRequest();
        return new RestaurantCreateRequest(
                valid.category(),
                valid.name(),
                valid.phone(),
                valid.description(),
                valid.address(),
                valid.addressDetail(),
                valid.zipCode(),
                "minOrderAmount".equals(property) ? null : valid.minOrderAmount(),
                "deliveryFee".equals(property) ? null : valid.deliveryFee(),
                "latitude".equals(property) ? null : valid.latitude(),
                "longitude".equals(property) ? null : valid.longitude(),
                "deliveryRadiusKm".equals(property) ? null : valid.deliveryRadiusKm()
        );
    }

    private static RestaurantCreateRequest createRequestWithInteger(String property, Integer value) {
        RestaurantCreateRequest valid = validCreateRequest();
        return new RestaurantCreateRequest(
                valid.category(),
                valid.name(),
                valid.phone(),
                valid.description(),
                valid.address(),
                valid.addressDetail(),
                valid.zipCode(),
                "minOrderAmount".equals(property) ? value : valid.minOrderAmount(),
                "deliveryFee".equals(property) ? value : valid.deliveryFee(),
                valid.latitude(),
                valid.longitude(),
                valid.deliveryRadiusKm()
        );
    }

    private static RestaurantCreateRequest createRequestWithDecimal(String property, BigDecimal value) {
        RestaurantCreateRequest valid = validCreateRequest();
        return new RestaurantCreateRequest(
                valid.category(),
                valid.name(),
                valid.phone(),
                valid.description(),
                valid.address(),
                valid.addressDetail(),
                valid.zipCode(),
                valid.minOrderAmount(),
                valid.deliveryFee(),
                "latitude".equals(property) ? value : valid.latitude(),
                "longitude".equals(property) ? value : valid.longitude(),
                "deliveryRadiusKm".equals(property) ? value : valid.deliveryRadiusKm()
        );
    }

    private static RestaurantUpdateRequest emptyUpdateRequest() {
        return new RestaurantUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static RestaurantUpdateRequest updateRequestWithString(String property, String value) {
        return new RestaurantUpdateRequest(
                "category".equals(property) ? value : null,
                "name".equals(property) ? value : null,
                "phone".equals(property) ? value : null,
                "description".equals(property) ? value : null,
                "address".equals(property) ? value : null,
                "addressDetail".equals(property) ? value : null,
                "zipCode".equals(property) ? value : null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static RestaurantUpdateRequest updateRequestWithInteger(String property, Integer value) {
        return new RestaurantUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "minOrderAmount".equals(property) ? value : null,
                "deliveryFee".equals(property) ? value : null,
                null,
                null,
                null
        );
    }

    private static RestaurantUpdateRequest updateRequestWithDecimal(String property, BigDecimal value) {
        return new RestaurantUpdateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "latitude".equals(property) ? value : null,
                "longitude".equals(property) ? value : null,
                "deliveryRadiusKm".equals(property) ? value : null
        );
    }

    /**
     * 주어진 요청 객체에서 단 하나의 특정 속성에 대한 유효성 검사 오류가 발생했는지 확인합니다.
     * 하나의 속성만 유효성 검사 오류를 진행하여, 의도하지 않은 유효성 검사 오류가 발생하지 않도록 확인합니다.
     *
     * @param request  유효성 검사를 수행할 객체
     * @param property 유효성 검사 오류가 발생할 것으로 예상되는 속성의 이름
     */
    private static void assertViolatesOnlyProperty(Object request, String property) {
        assertThat(validator.validate(request))
                .isNotEmpty()
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsOnly(property);
    }

    /**
     * 주어진 요청 객체에서 특정 속성에 대해 {@code @Digits} 제약 조건 유효성 검사 오류가 발생했는지 확인합니다.
     *
     * @param request  유효성 검사를 수행할 객체
     * @param property 유효성 검사 오류가 발생할 것으로 예상되는 속성의 이름
     */
    private static void assertDigitsViolation(Object request, String property) {
        assertThat(validator.validate(request))
                .filteredOn(violation -> violation.getPropertyPath().toString().equals(property))
                .<Class<?>>extracting(RestaurantRequestValidationTest::constraintType)
                .contains(Digits.class);
    }

    private static Class<?> constraintType(ConstraintViolation<Object> violation) {
        return violation.getConstraintDescriptor().getAnnotation().annotationType();
    }
}
