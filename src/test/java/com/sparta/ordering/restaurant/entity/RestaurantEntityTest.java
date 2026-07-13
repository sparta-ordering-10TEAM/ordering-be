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
