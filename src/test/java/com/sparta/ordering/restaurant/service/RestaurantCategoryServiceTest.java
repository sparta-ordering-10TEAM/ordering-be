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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verifyNoInteractions;
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
        @DisplayName("활성 카테고리 목록을 매핑해 반환한다")
        void successReturnsMappedCategories() {
            RestaurantCategory korean = category("한식");
            RestaurantCategory chicken = category("치킨");
            when(restaurantCategoryRepository.findByDeletedAtIsNull()).thenReturn(List.of(korean, chicken));

            List<CategoryResponse> result = restaurantCategoryService.getCategories();

            assertThat(result).containsExactly(
                    CategoryResponse.from(korean),
                    CategoryResponse.from(chicken)
            );
            verify(restaurantCategoryRepository).findByDeletedAtIsNull();
        }

        @Test
        @DisplayName("활성 카테고리가 없으면 빈 목록을 반환한다")
        void successReturnsEmptyList() {
            when(restaurantCategoryRepository.findByDeletedAtIsNull()).thenReturn(List.of());

            List<CategoryResponse> result = restaurantCategoryService.getCategories();

            assertThat(result).isEmpty();
            verify(restaurantCategoryRepository).findByDeletedAtIsNull();
        }
    }

    @Nested
    @DisplayName("카테고리 생성")
    class CreateCategory {

        @ParameterizedTest(name = "[{index}] role={0}")
        @EnumSource(value = Role.class, names = {"MASTER", "MANAGER"})
        @DisplayName("MASTER/MANAGER는 카테고리를 생성할 수 있다")
        void successAdminCreatesCategory(Role role) {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            User admin = user(role);
            ReflectionTestUtils.setField(admin, "id", userId);
            CategoryCreateRequest request = new CategoryCreateRequest("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull("한식")).thenReturn(false);
            when(restaurantCategoryRepository.save(any(RestaurantCategory.class))).thenAnswer(invocation -> {
                RestaurantCategory saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", categoryId);
                return saved;
            });

            CategoryResponse response = restaurantCategoryService.createCategory(request, userId);

            ArgumentCaptor<RestaurantCategory> captor = ArgumentCaptor.forClass(RestaurantCategory.class);
            verify(restaurantCategoryRepository).save(captor.capture());
            assertThat(captor.getValue().getCode()).isEqualTo("한식");
            assertThat(response.categoryId()).isEqualTo(categoryId);
            assertThat(response.code()).isEqualTo("한식");
        }

        @Test
        @DisplayName("이미 존재하는 코드로 카테고리를 생성할 수 없다")
        void failWhenCodeAlreadyExists() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            CategoryCreateRequest request = new CategoryCreateRequest("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull("한식")).thenReturn(true);

            assertThatThrownBy(() -> restaurantCategoryService.createCategory(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue(
                            "responseCode",
                            GeneralResponseCode.RESTAURANT_CATEGORY_ALREADY_EXISTS
                    );

            verify(restaurantCategoryRepository, never()).save(any(RestaurantCategory.class));
        }

        @ParameterizedTest(name = "[{index}] role={0}")
        @EnumSource(value = Role.class, names = {"CUSTOMER", "OWNER"})
        @DisplayName("admin이 아닌 사용자는 카테고리를 생성할 수 없다")
        void failWhenNotAdmin(Role role) {
            UUID userId = UUID.randomUUID();
            User nonAdmin = user(role);
            ReflectionTestUtils.setField(nonAdmin, "id", userId);
            CategoryCreateRequest request = new CategoryCreateRequest("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(nonAdmin));

            assertThatThrownBy(() -> restaurantCategoryService.createCategory(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(restaurantCategoryRepository);
        }

        @Test
        @DisplayName("요청 사용자가 존재하지 않으면 카테고리를 생성할 수 없다")
        void failUserNotFound() {
            UUID userId = UUID.randomUUID();
            CategoryCreateRequest request = new CategoryCreateRequest("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantCategoryService.createCategory(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.USER_NOT_FOUND);

            verifyNoInteractions(restaurantCategoryRepository);
        }
    }

    @Nested
    @DisplayName("카테고리 수정")
    class UpdateCategory {

        @Test
        @DisplayName("admin은 카테고리 코드를 수정할 수 있다")
        void successAdminUpdatesCategory() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MANAGER);
            ReflectionTestUtils.setField(admin, "id", userId);
            RestaurantCategory category = category("한식");
            CategoryUpdateRequest request = new CategoryUpdateRequest("중식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));
            when(restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull("중식")).thenReturn(false);

            CategoryResponse response = restaurantCategoryService.updateCategory(category.getId(), request, userId);

            assertThat(category.getCode()).isEqualTo("중식");
            assertThat(response).isEqualTo(CategoryResponse.from(category));
        }

        @Test
        @DisplayName("동일한 코드로 수정하면 중복 검사 없이 성공한다")
        void successSameCodeNoOp() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            RestaurantCategory category = category("한식");
            CategoryUpdateRequest request = new CategoryUpdateRequest("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));

            CategoryResponse response = restaurantCategoryService.updateCategory(category.getId(), request, userId);

            assertThat(category.getCode()).isEqualTo("한식");
            assertThat(response.code()).isEqualTo("한식");
            verify(restaurantCategoryRepository, never()).existsByCodeAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("다른 카테고리가 이미 쓰는 코드로 수정할 수 없다")
        void failWhenOtherCodeAlreadyExists() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            RestaurantCategory category = category("한식");
            CategoryUpdateRequest request = new CategoryUpdateRequest("치킨");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));
            when(restaurantCategoryRepository.existsByCodeAndDeletedAtIsNull("치킨")).thenReturn(true);

            assertThatThrownBy(() -> restaurantCategoryService.updateCategory(category.getId(), request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue(
                            "responseCode",
                            GeneralResponseCode.RESTAURANT_CATEGORY_ALREADY_EXISTS
                    );

            assertThat(category.getCode()).isEqualTo("한식");
        }

        @Test
        @DisplayName("존재하지 않는 카테고리는 수정할 수 없다")
        void failCategoryNotFound() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            CategoryUpdateRequest request = new CategoryUpdateRequest("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantCategoryService.updateCategory(categoryId, request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue(
                            "responseCode",
                            GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND
                    );
        }

        @Test
        @DisplayName("admin이 아니면 카테고리를 수정할 수 없다")
        void failWhenNotAdmin() {
            UUID userId = UUID.randomUUID();
            User owner = user(Role.OWNER);
            ReflectionTestUtils.setField(owner, "id", userId);
            CategoryUpdateRequest request = new CategoryUpdateRequest("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> restaurantCategoryService.updateCategory(UUID.randomUUID(), request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(restaurantCategoryRepository);
        }
    }

    @Nested
    @DisplayName("카테고리 삭제")
    class DeleteCategory {

        @Test
        @DisplayName("admin은 사용 중이 아닌 카테고리를 삭제할 수 있다")
        void successAdminDeletesCategory() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            RestaurantCategory category = category("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));
            when(restaurantRepository.existsByCategory_IdAndDeletedAtIsNull(category.getId())).thenReturn(false);

            restaurantCategoryService.deleteCategory(category.getId(), userId);

            assertThat(category.getDeletedBy()).isEqualTo(userId);
            assertThat(category.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("음식점에서 사용 중인 카테고리는 삭제할 수 없다")
        void failWhenCategoryInUse() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MANAGER);
            ReflectionTestUtils.setField(admin, "id", userId);
            RestaurantCategory category = category("한식");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(category.getId()))
                    .thenReturn(Optional.of(category));
            when(restaurantRepository.existsByCategory_IdAndDeletedAtIsNull(category.getId())).thenReturn(true);

            assertThatThrownBy(() -> restaurantCategoryService.deleteCategory(category.getId(), userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_CATEGORY_IN_USE);

            assertThat(category.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 카테고리는 삭제할 수 없다")
        void failCategoryNotFound() {
            UUID userId = UUID.randomUUID();
            UUID categoryId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(restaurantCategoryRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantCategoryService.deleteCategory(categoryId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue(
                            "responseCode",
                            GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND
                    );

            verifyNoInteractions(restaurantRepository);
        }

        @Test
        @DisplayName("admin이 아니면 카테고리를 삭제할 수 없다")
        void failWhenNotAdmin() {
            UUID userId = UUID.randomUUID();
            User customer = user(Role.CUSTOMER);
            ReflectionTestUtils.setField(customer, "id", userId);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> restaurantCategoryService.deleteCategory(UUID.randomUUID(), userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(restaurantCategoryRepository, restaurantRepository);
        }
    }

    private static RestaurantCategory category(String code) {
        RestaurantCategory category = RestaurantCategory.builder().code(code).build();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        return category;
    }

    private static User user(Role role) {
        return User.builder()
                .userName("user-" + UUID.randomUUID())
                .nickName("user")
                .phoneNumber("010-0000-0000")
                .role(role)
                .password("password")
                .build();
    }
}
