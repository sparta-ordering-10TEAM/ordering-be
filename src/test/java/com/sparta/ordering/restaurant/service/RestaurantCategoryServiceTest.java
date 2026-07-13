package com.sparta.ordering.restaurant.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.dto.CategoryCreateRequest;
import com.sparta.ordering.restaurant.dto.CategoryResponse;
import com.sparta.ordering.restaurant.dto.CategoryUpdateRequest;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.repository.RestaurantCategoryRepository;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantCategoryServiceTest {

    @Mock
    private RestaurantCategoryRepository restaurantCategoryRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RestaurantCategoryService restaurantCategoryService;

    @Nested
    @DisplayName("카테고리 목록 조회")
    class GetCategories {

        @Test
        @DisplayName("삭제되지 않은 카테고리를 전체 조회한다")
        void success() {
            RestaurantCategory korean = category("한식");
            RestaurantCategory chicken = category("치킨");

            when(restaurantCategoryRepository.findByDeletedAtIsNull()).thenReturn(List.of(korean, chicken));

            List<CategoryResponse> result = restaurantCategoryService.getCategories();

            assertThat(result).containsExactly(CategoryResponse.from(korean), CategoryResponse.from(chicken));
        }
    }

    @Nested
    @DisplayName("카테고리 생성")
    class CreateCategory {

        @Test
        @DisplayName("MANAGER는 카테고리를 생성할 수 있다")
        void successManagerCreatesCategory() {
            UUID managerId = adminUser(Role.MANAGER);
            CategoryCreateRequest request = new CategoryCreateRequest("일식");

            when(restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull("일식")).thenReturn(false);
            when(restaurantCategoryRepository.save(any(RestaurantCategory.class)))
                    .thenAnswer(invocation -> {
                        RestaurantCategory saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
                        return saved;
                    });

            CategoryResponse response = restaurantCategoryService.createCategory(request, managerId);

            assertThat(response.code()).isEqualTo("일식");
        }

        @Test
        @DisplayName("이미 존재하는 코드로 카테고리를 생성할 수 없다")
        void failDuplicatedCode() {
            UUID managerId = adminUser(Role.MANAGER);
            CategoryCreateRequest request = new CategoryCreateRequest("한식");

            when(restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull("한식")).thenReturn(true);

            assertThatThrownBy(() -> restaurantCategoryService.createCategory(request, managerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue(
                            "responseCode",
                            GeneralResponseCode.RESTAURANT_CATEGORY_ALREADY_EXISTS
                    );

            verify(restaurantCategoryRepository, never()).save(any(RestaurantCategory.class));
        }

        @ParameterizedTest(name = "[{index}] role={0}")
        @EnumSource(value = Role.class, names = {"CUSTOMER", "OWNER"})
        @DisplayName("MANAGER, MASTER가 아닌 사용자는 카테고리를 생성할 수 없다")
        void failNotAdmin(Role role) {
            UUID userId = adminUser(role);
            CategoryCreateRequest request = new CategoryCreateRequest("일식");

            assertThatThrownBy(() -> restaurantCategoryService.createCategory(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verify(restaurantCategoryRepository, never()).save(any(RestaurantCategory.class));
        }
    }

    @Nested
    @DisplayName("카테고리 수정")
    class UpdateCategory {

        @Test
        @DisplayName("MASTER는 카테고리 코드를 수정할 수 있다")
        void successUpdateCode() {
            UUID masterId = adminUser(Role.MASTER);
            RestaurantCategory category = category("분식");

            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));
            when(restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull("야식")).thenReturn(false);

            CategoryResponse response = restaurantCategoryService.updateCategory(
                    category.getId(),
                    new CategoryUpdateRequest("야식"),
                    masterId
            );

            assertThat(response.code()).isEqualTo("야식");
            assertThat(category.getCode()).isEqualTo("야식");
        }

        @Test
        @DisplayName("이미 존재하는 코드로 수정할 수 없다")
        void failDuplicatedCode() {
            UUID masterId = adminUser(Role.MASTER);
            RestaurantCategory category = category("분식");

            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));
            when(restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull("한식")).thenReturn(true);

            assertThatThrownBy(() -> restaurantCategoryService.updateCategory(
                    category.getId(),
                    new CategoryUpdateRequest("한식"),
                    masterId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue(
                            "responseCode",
                            GeneralResponseCode.RESTAURANT_CATEGORY_ALREADY_EXISTS
                    );

            assertThat(category.getCode()).isEqualTo("분식");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리는 수정할 수 없다")
        void failCategoryNotFound() {
            UUID masterId = adminUser(Role.MASTER);
            UUID categoryId = UUID.randomUUID();

            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantCategoryService.updateCategory(
                    categoryId,
                    new CategoryUpdateRequest("한식"),
                    masterId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("카테고리 삭제")
    class DeleteCategory {

        @Test
        @DisplayName("MANAGER는 사용 중이 아닌 카테고리를 soft delete 할 수 있다")
        void successSoftDelete() {
            UUID managerId = adminUser(Role.MANAGER);
            RestaurantCategory category = category("분식");

            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));
            when(restaurantRepository.existsByCategory_IdAndDeletedAtIsNull(category.getId())).thenReturn(false);

            restaurantCategoryService.deleteCategory(category.getId(), managerId);

            assertThat(category.getDeletedAt()).isNotNull();
            assertThat(category.getDeletedBy()).isEqualTo(managerId);
        }

        @Test
        @DisplayName("사용 중인 카테고리는 삭제할 수 없다")
        void failCategoryInUse() {
            UUID managerId = adminUser(Role.MANAGER);
            RestaurantCategory category = category("한식");

            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));
            when(restaurantRepository.existsByCategory_IdAndDeletedAtIsNull(category.getId())).thenReturn(true);

            assertThatThrownBy(() -> restaurantCategoryService.deleteCategory(category.getId(), managerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_CATEGORY_IN_USE);

            assertThat(category.getDeletedAt()).isNull();
        }
    }

    private UUID adminUser(Role role) {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userName("user-" + userId)
                .nickName("user")
                .phoneNumber("010-0000-0000")
                .role(role)
                .password("password")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        return userId;
    }

    private static RestaurantCategory category(String code) {
        RestaurantCategory category = RestaurantCategory.builder().code(code).build();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        return category;
    }
}
