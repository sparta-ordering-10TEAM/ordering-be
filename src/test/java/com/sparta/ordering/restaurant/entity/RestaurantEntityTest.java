package com.sparta.ordering.restaurant.entity;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestaurantEntityTest {

    @Nested
    @DisplayName("음식점 정보 수정")
    class Update {

        @Test
        @DisplayName("전달된 모든 필드를 새로운 값으로 수정한다")
        void updatesAllSuppliedFields() {
            Restaurant restaurant = restaurant(owner(UUID.randomUUID()), restaurantCategory("한식"));
            RestaurantCategory newCategory = restaurantCategory("치킨");

            restaurant.update(
                    newCategory,
                    "새 음식점",
                    "02-9876-5432",
                    "새 설명",
                    "부산시 해운대구",
                    "3층",
                    "48000",
                    20000,
                    4500,
                    new BigDecimal("35.1234567"),
                    new BigDecimal("129.1234567"),
                    new BigDecimal("5.5")
            );

            assertThat(restaurant.getCategory()).isSameAs(newCategory);
            assertThat(restaurant.getName()).isEqualTo("새 음식점");
            assertThat(restaurant.getPhone()).isEqualTo("02-9876-5432");
            assertThat(restaurant.getDescription()).isEqualTo("새 설명");
            assertThat(restaurant.getAddress()).isEqualTo("부산시 해운대구");
            assertThat(restaurant.getAddressDetail()).isEqualTo("3층");
            assertThat(restaurant.getZipCode()).isEqualTo("48000");
            assertThat(restaurant.getMinOrderAmount()).isEqualTo(20000);
            assertThat(restaurant.getDeliveryFee()).isEqualTo(4500);
            assertThat(restaurant.getLatitude()).isEqualByComparingTo("35.1234567");
            assertThat(restaurant.getLongitude()).isEqualByComparingTo("129.1234567");
            assertThat(restaurant.getDeliveryRadiusKm()).isEqualByComparingTo("5.5");
        }

        @Test
        @DisplayName("null로 전달된 모든 필드는 기존 값을 유지한다")
        void preservesAllFieldsWhenEverySuppliedValueIsNull() {
            User owner = owner(UUID.randomUUID());
            RestaurantCategory category = restaurantCategory("한식");
            Restaurant restaurant = restaurant(owner, category);

            restaurant.update(
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

            assertThat(restaurant.getUser()).isSameAs(owner);
            assertThat(restaurant.getCategory()).isSameAs(category);
            assertThat(restaurant.getName()).isEqualTo("기존 음식점");
            assertThat(restaurant.getPhone()).isEqualTo("02-1234-5678");
            assertThat(restaurant.getDescription()).isEqualTo("기존 설명");
            assertThat(restaurant.getAddress()).isEqualTo("서울시 강남구");
            assertThat(restaurant.getAddressDetail()).isEqualTo("2층");
            assertThat(restaurant.getZipCode()).isEqualTo("12345");
            assertThat(restaurant.getMinOrderAmount()).isEqualTo(15000);
            assertThat(restaurant.getDeliveryFee()).isEqualTo(3500);
            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.CLOSED);
            assertThat(restaurant.getLatitude()).isEqualByComparingTo("37.1234567");
            assertThat(restaurant.getLongitude()).isEqualByComparingTo("127.1234567");
            assertThat(restaurant.getDeliveryRadiusKm()).isEqualByComparingTo("4.5");
        }

        @Test
        @DisplayName("일부 필드만 전달하면 해당 필드만 수정하고 나머지는 유지한다")
        void updatesOnlySuppliedFields() {
            User owner = owner(UUID.randomUUID());
            RestaurantCategory category = restaurantCategory("한식");
            Restaurant restaurant = restaurant(owner, category);

            restaurant.update(
                    null,
                    "부분 수정 음식점",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    5000,
                    null,
                    null,
                    null
            );

            assertThat(restaurant.getName()).isEqualTo("부분 수정 음식점");
            assertThat(restaurant.getDeliveryFee()).isEqualTo(5000);
            assertThat(restaurant.getUser()).isSameAs(owner);
            assertThat(restaurant.getCategory()).isSameAs(category);
            assertThat(restaurant.getPhone()).isEqualTo("02-1234-5678");
            assertThat(restaurant.getDescription()).isEqualTo("기존 설명");
            assertThat(restaurant.getAddress()).isEqualTo("서울시 강남구");
            assertThat(restaurant.getAddressDetail()).isEqualTo("2층");
            assertThat(restaurant.getZipCode()).isEqualTo("12345");
            assertThat(restaurant.getMinOrderAmount()).isEqualTo(15000);
            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.CLOSED);
            assertThat(restaurant.getLatitude()).isEqualByComparingTo("37.1234567");
            assertThat(restaurant.getLongitude()).isEqualByComparingTo("127.1234567");
            assertThat(restaurant.getDeliveryRadiusKm()).isEqualByComparingTo("4.5");
        }

        @Test
        @DisplayName("빈 문자열 설명을 전달하면 설명을 비운다")
        void clearsDescriptionWithEmptyString() {
            Restaurant restaurant = restaurant(owner(UUID.randomUUID()), restaurantCategory("한식"));

            restaurant.update(
                    null,
                    null,
                    null,
                    "",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThat(restaurant.getDescription()).isEmpty();
        }
    }

    @Nested
    @DisplayName("음식점 영업 상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("새 영업 상태로 변경한다")
        void changesStatus() {
            Restaurant restaurant = restaurant(owner(UUID.randomUUID()), restaurantCategory("한식"));

            restaurant.changeStatus(RestaurantStatus.OPEN);

            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.OPEN);
        }

        @Test
        @DisplayName("null 상태는 거부하고 기존 상태를 유지한다")
        void rejectsNullAndPreservesStatus() {
            Restaurant restaurant = restaurant(owner(UUID.randomUUID()), restaurantCategory("한식"));

            assertThatThrownBy(() -> restaurant.changeStatus(null))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue(
                            "responseCode",
                            GeneralResponseCode.RESTAURANT_STATUS_INVALID
                    );
            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("음식점 소유자 확인")
    class IsOwnedBy {

        @Test
        @DisplayName("서로 다른 사용자 객체라도 UUID가 같으면 소유자다")
        void returnsTrueForDistinctUserWithSameId() {
            UUID ownerId = UUID.randomUUID();
            Restaurant restaurant = restaurant(owner(ownerId), restaurantCategory("한식"));
            User candidate = owner(ownerId);

            assertThat(restaurant.isOwnedBy(candidate)).isTrue();
        }

        @Test
        @DisplayName("사용자 UUID가 다르면 소유자가 아니다")
        void returnsFalseForDifferentId() {
            Restaurant restaurant = restaurant(owner(UUID.randomUUID()), restaurantCategory("한식"));
            User candidate = owner(UUID.randomUUID());

            assertThat(restaurant.isOwnedBy(candidate)).isFalse();
        }

        @Test
        @DisplayName("사용자가 null이면 소유자가 아니다")
        void returnsFalseForNullUser() {
            Restaurant restaurant = restaurant(owner(UUID.randomUUID()), restaurantCategory("한식"));

            assertThat(restaurant.isOwnedBy(null)).isFalse();
        }

        @Test
        @DisplayName("음식점 소유자의 UUID가 null이면 소유자가 아니다")
        void returnsFalseWhenRestaurantOwnerIdIsNull() {
            Restaurant restaurant = restaurant(owner(null), restaurantCategory("한식"));
            User candidate = owner(UUID.randomUUID());

            assertThat(restaurant.isOwnedBy(candidate)).isFalse();
        }

        @Test
        @DisplayName("후보 사용자의 UUID가 null이면 소유자가 아니다")
        void returnsFalseWhenCandidateIdIsNull() {
            Restaurant restaurant = restaurant(owner(UUID.randomUUID()), restaurantCategory("한식"));
            User candidate = owner(null);

            assertThat(restaurant.isOwnedBy(candidate)).isFalse();
        }

        @Test
        @DisplayName("음식점 소유자와 후보 사용자의 UUID가 모두 null이어도 소유자가 아니다")
        void returnsFalseWhenBothIdsAreNull() {
            Restaurant restaurant = restaurant(owner(null), restaurantCategory("한식"));
            User candidate = owner(null);

            assertThat(restaurant.isOwnedBy(candidate)).isFalse();
        }
    }

    private static User owner(UUID id) {
        User owner = User.builder()
                .userName("owner-" + UUID.randomUUID())
                .nickName("음식점 사장")
                .email(UUID.randomUUID() + "@example.com")
                .phoneNumber("010-1234-5678")
                .role(Role.OWNER)
                .password("password")
                .build();
        ReflectionTestUtils.setField(owner, "id", id);
        return owner;
    }

    private static RestaurantCategory restaurantCategory(String code) {
        RestaurantCategory category = new RestaurantCategory();
        ReflectionTestUtils.setField(category, "code", code);
        return category;
    }

    private static Restaurant restaurant(User owner, RestaurantCategory category) {
        return Restaurant.builder()
                .user(owner)
                .category(category)
                .name("기존 음식점")
                .phone("02-1234-5678")
                .description("기존 설명")
                .address("서울시 강남구")
                .addressDetail("2층")
                .zipCode("12345")
                .minOrderAmount(15000)
                .deliveryFee(3500)
                .status(RestaurantStatus.CLOSED)
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .deliveryRadiusKm(new BigDecimal("4.5"))
                .build();
    }
}
