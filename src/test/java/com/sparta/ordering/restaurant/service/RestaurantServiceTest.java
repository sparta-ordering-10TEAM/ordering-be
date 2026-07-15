package com.sparta.ordering.restaurant.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.dto.RestaurantCreateRequest;
import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.dto.RestaurantUpdateRequest;
import com.sparta.ordering.restaurant.entity.Region;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import com.sparta.ordering.restaurant.repository.RegionRepository;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
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
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantCategoryRepository restaurantCategoryRepository;

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    @Nested
    @DisplayName("음식점 목록 조회")
    class GetRestaurants {

        @ParameterizedTest(name = "[{index}] category=''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("카테고리가 없거나 공백이면 필터 없이 음식점을 조회한다")
        void getAllActiveRestaurants(String category) {
            Pageable pageable = PageRequest.of(0, 10);
            RestaurantCategory korean = restaurantCategory("한식");
            Restaurant restaurant = restaurant(korean, "한식당");
            Page<Restaurant> restaurantPage = new PageImpl<>(List.of(restaurant), pageable, 1);

            when(restaurantRepository.findWithFilters(null, null, null, pageable)).thenReturn(restaurantPage);

            Page<RestaurantResponse> result = restaurantService.getRestaurants(category, null, null, pageable);

            assertThat(result.getContent()).containsExactly(RestaurantResponse.from(restaurant));
            verify(restaurantRepository).findWithFilters(null, null, null, pageable);
            verifyNoInteractions(restaurantCategoryRepository);
        }

        @Test
        @DisplayName("카테고리가 있으면 해당 카테고리 ID로 필터 조회한다")
        void getActiveRestaurantsByCategory() {
            Pageable pageable = PageRequest.of(0, 10);
            RestaurantCategory chicken = restaurantCategory("치킨");
            Restaurant restaurant = restaurant(chicken, "치킨집");
            Page<Restaurant> restaurantPage = new PageImpl<>(List.of(restaurant), pageable, 1);

            when(restaurantCategoryRepository.findByCodeAndDeletedAtIsNull("치킨"))
                    .thenReturn(Optional.of(chicken));
            when(restaurantRepository.findWithFilters(chicken.getId(), null, null, pageable))
                    .thenReturn(restaurantPage);

            Page<RestaurantResponse> result = restaurantService.getRestaurants("치킨", null, null, pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).restaurantId()).isEqualTo(restaurant.getId());
            assertThat(result.getContent().get(0).category()).isEqualTo("치킨");
            assertThat(result.getContent().get(0).name()).isEqualTo("치킨집");
            verify(restaurantCategoryRepository).findByCodeAndDeletedAtIsNull("치킨");
            verify(restaurantRepository).findWithFilters(chicken.getId(), null, null, pageable);
        }

        @Test
        @DisplayName("지역과 상태 필터가 있으면 findWithFilters에 동일한 값이 전달된다")
        void getRestaurantsWithRegionAndStatusFilters() {
            Pageable pageable = PageRequest.of(0, 10);
            UUID regionId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            Page<Restaurant> restaurantPage = new PageImpl<>(List.of(restaurant), pageable, 1);

            when(restaurantRepository.findWithFilters(null, regionId, RestaurantStatus.OPEN, pageable))
                    .thenReturn(restaurantPage);

            Page<RestaurantResponse> result = restaurantService.getRestaurants(
                    null,
                    regionId,
                    RestaurantStatus.OPEN,
                    pageable
            );

            assertThat(result.getContent()).hasSize(1);
            verify(restaurantRepository).findWithFilters(null, regionId, RestaurantStatus.OPEN, pageable);
            verifyNoInteractions(restaurantCategoryRepository);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리로 음식점을 조회할 수 없다")
        void failRestaurantCategoryNotFound() {
            Pageable pageable = PageRequest.of(0, 10);
            when(restaurantCategoryRepository.findByCodeAndDeletedAtIsNull("UNKNOWN"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.getRestaurants("UNKNOWN", null, null, pageable))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND);

            verifyNoInteractions(restaurantRepository);
        }
    }

    @Nested
    @DisplayName("음식점 상세 조회")
    class GetRestaurant {

        @Test
        @DisplayName("존재하는 음식점의 상세 정보를 조회한다")
        void success() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("피자"), "피자집");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));

            RestaurantResponse response = restaurantService.getRestaurant(restaurantId);

            assertThat(response.restaurantId()).isEqualTo(restaurantId);
            assertThat(response.category()).isEqualTo("피자");
            assertThat(response.name()).isEqualTo("피자집");
            assertThat(response.regionId()).isEqualTo(restaurant.getRegion().getId());
            assertThat(response.regionName()).isEqualTo(restaurant.getRegion().getName());
        }

        @Test
        @DisplayName("존재하지 않는 음식점은 조회할 수 없다")
        void failRestaurantNotFound() {
            UUID restaurantId = UUID.randomUUID();
            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.getRestaurant(restaurantId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("내 음식점 목록 조회")
    class GetOwnerRestaurants {

        @Test
        @DisplayName("OWNER의 삭제되지 않은 음식점을 페이지로 조회한다")
        void successReturnsOwnerRestaurants() {
            UUID ownerId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(1, 5);
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant.getUser(), "id", ownerId);
            Page<Restaurant> restaurantPage = new PageImpl<>(List.of(restaurant), pageable, 6);

            when(restaurantRepository.findByUser_IdAndDeletedAtIsNull(ownerId, pageable))
                    .thenReturn(restaurantPage);

            Page<RestaurantResponse> result = restaurantService.getOwnerRestaurants(ownerId, pageable);

            assertThat(result.getContent()).containsExactly(RestaurantResponse.from(restaurant));
            assertThat(result.getTotalElements()).isEqualTo(6);
            verify(restaurantRepository).findByUser_IdAndDeletedAtIsNull(ownerId, pageable);
        }

        @Test
        @DisplayName("조회 결과가 없어도 빈 페이지를 반환한다")
        void successReturnsEmptyPage() {
            UUID ownerId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);
            when(restaurantRepository.findByUser_IdAndDeletedAtIsNull(ownerId, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<RestaurantResponse> result = restaurantService.getOwnerRestaurants(ownerId, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(restaurantRepository).findByUser_IdAndDeletedAtIsNull(ownerId, pageable);
        }
    }

    @Nested
    @DisplayName("음식점 생성")
    class CreateRestaurant {

        @Test
        @DisplayName("OWNER가 음식점을 생성하면 CLOSED 상태로 저장한다")
        void successOwnerCreatesRestaurant() {
            UUID ownerId = UUID.randomUUID();
            UUID restaurantId = UUID.randomUUID();
            User owner = user(Role.OWNER);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            RestaurantCategory category = restaurantCategory("한식");
            Region region = leafRegion("역삼동");
            RestaurantCreateRequest request = validRestaurantCreateRequest(region.getId());

            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantCategoryRepository.findByCodeAndDeletedAtIsNull(request.category()))
                    .thenReturn(Optional.of(category));
            when(regionRepository.findByIdAndDeletedAtIsNull(region.getId())).thenReturn(Optional.of(region));
            when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
                Restaurant savedRestaurant = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedRestaurant, "id", restaurantId);
                return savedRestaurant;
            });

            RestaurantResponse response = restaurantService.createRestaurant(request, ownerId);

            ArgumentCaptor<Restaurant> captor = ArgumentCaptor.forClass(Restaurant.class);
            verify(restaurantRepository).save(captor.capture());
            Restaurant savedRestaurant = captor.getValue();
            assertThat(savedRestaurant.getUser()).isSameAs(owner);
            assertThat(savedRestaurant.getCategory()).isSameAs(category);
            assertThat(savedRestaurant.getRegion()).isSameAs(region);
            assertThat(savedRestaurant.getName()).isEqualTo(request.name());
            assertThat(savedRestaurant.getPhone()).isEqualTo(request.phone());
            assertThat(savedRestaurant.getDescription()).isEqualTo(request.description());
            assertThat(savedRestaurant.getAddress()).isEqualTo(request.address());
            assertThat(savedRestaurant.getAddressDetail()).isEqualTo(request.addressDetail());
            assertThat(savedRestaurant.getZipCode()).isEqualTo(request.zipCode());
            assertThat(savedRestaurant.getMinOrderAmount()).isEqualTo(request.minOrderAmount());
            assertThat(savedRestaurant.getDeliveryFee()).isEqualTo(request.deliveryFee());
            assertThat(savedRestaurant.getStatus()).isEqualTo(RestaurantStatus.CLOSED);
            assertThat(savedRestaurant.getLatitude()).isEqualByComparingTo(request.latitude());
            assertThat(savedRestaurant.getLongitude()).isEqualByComparingTo(request.longitude());
            assertThat(savedRestaurant.getDeliveryRadiusKm()).isEqualByComparingTo(request.deliveryRadiusKm());
            assertThat(response).isEqualTo(RestaurantResponse.from(savedRestaurant));
            assertThat(response.restaurantId()).isEqualTo(restaurantId);
            assertThat(response.regionId()).isEqualTo(region.getId());
            assertThat(response.regionName()).isEqualTo(region.getName());
        }

        @Test
        @DisplayName("생성 요청 사용자가 존재하지 않으면 음식점을 생성할 수 없다")
        void failUserNotFound() {
            UUID userId = UUID.randomUUID();
            RestaurantCreateRequest request = validRestaurantCreateRequest(UUID.randomUUID());
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.createRestaurant(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.USER_NOT_FOUND);

            verifyNoInteractions(restaurantCategoryRepository, regionRepository, restaurantRepository);
        }

        @ParameterizedTest(name = "[{index}] DB 권한={0}")
        @EnumSource(value = Role.class, names = "OWNER", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("DB 권한이 OWNER가 아닌 사용자는 음식점을 생성할 수 없다")
        void failWhenDbRoleIsNotOwner(Role role) {
            UUID userId = UUID.randomUUID();
            User requestUser = user(role);
            ReflectionTestUtils.setField(requestUser, "id", userId);
            RestaurantCreateRequest request = validRestaurantCreateRequest(UUID.randomUUID());
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(requestUser));

            assertThatThrownBy(() -> restaurantService.createRestaurant(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(restaurantCategoryRepository, regionRepository, restaurantRepository);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리로 음식점을 생성할 수 없다")
        void failRestaurantCategoryNotFound() {
            UUID ownerId = UUID.randomUUID();
            User owner = user(Role.OWNER);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            RestaurantCreateRequest request = validRestaurantCreateRequest(UUID.randomUUID());
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantCategoryRepository.findByCodeAndDeletedAtIsNull(request.category()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.createRestaurant(request, ownerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue(
                            "responseCode",
                            GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND
                    );

            verify(restaurantRepository, never()).save(any(Restaurant.class));
            verifyNoInteractions(regionRepository);
        }

        @Test
        @DisplayName("존재하지 않는 지역으로 음식점을 생성할 수 없다")
        void failRegionNotFound() {
            UUID ownerId = UUID.randomUUID();
            User owner = user(Role.OWNER);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            RestaurantCategory category = restaurantCategory("한식");
            UUID regionId = UUID.randomUUID();
            RestaurantCreateRequest request = validRestaurantCreateRequest(regionId);

            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantCategoryRepository.findByCodeAndDeletedAtIsNull(request.category()))
                    .thenReturn(Optional.of(category));
            when(regionRepository.findByIdAndDeletedAtIsNull(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.createRestaurant(request, ownerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_NOT_FOUND);

            verify(restaurantRepository, never()).save(any(Restaurant.class));
        }

        @Test
        @DisplayName("최하위 지역이 아니면 음식점을 생성할 수 없다")
        void failRegionNotLeaf() {
            UUID ownerId = UUID.randomUUID();
            User owner = user(Role.OWNER);
            ReflectionTestUtils.setField(owner, "id", ownerId);
            RestaurantCategory category = restaurantCategory("한식");
            Region region = nonLeafRegion("강남구");
            RestaurantCreateRequest request = validRestaurantCreateRequest(region.getId());

            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantCategoryRepository.findByCodeAndDeletedAtIsNull(request.category()))
                    .thenReturn(Optional.of(category));
            when(regionRepository.findByIdAndDeletedAtIsNull(region.getId())).thenReturn(Optional.of(region));

            assertThatThrownBy(() -> restaurantService.createRestaurant(request, ownerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_NOT_LEAF);

            verify(restaurantRepository, never()).save(any(Restaurant.class));
        }
    }

    @Nested
    @DisplayName("음식점 정보 수정")
    class UpdateRestaurant {

        @Test
        @DisplayName("OWNER는 본인 음식점 정보를 수정할 수 있다")
        void successOwnerUpdatesOwnRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            RestaurantCategory oldCategory = restaurantCategory("한식");
            RestaurantCategory newCategory = restaurantCategory("치킨");
            Region newRegion = leafRegion("삼성동");
            Restaurant restaurant = restaurant(oldCategory, "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID ownerId = restaurant.getUser().getId();
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    "치킨",
                    newRegion.getId(),
                    "치킨집",
                    "02-1111-2222",
                    "바삭한 치킨",
                    "서울시 강남구",
                    "2층",
                    "12345",
                    15000,
                    3500,
                    new BigDecimal("37.7654321"),
                    new BigDecimal("127.7654321"),
                    new BigDecimal("4.5")
            );

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(restaurant.getUser()));
            when(restaurantCategoryRepository.findByCodeAndDeletedAtIsNull("치킨"))
                    .thenReturn(Optional.of(newCategory));
            when(regionRepository.findByIdAndDeletedAtIsNull(newRegion.getId())).thenReturn(Optional.of(newRegion));

            RestaurantResponse response = restaurantService.updateRestaurant(restaurantId, request, ownerId);

            assertThat(response.restaurantId()).isEqualTo(restaurantId);
            assertThat(response.category()).isEqualTo("치킨");
            assertThat(response.regionId()).isEqualTo(newRegion.getId());
            assertThat(response.regionName()).isEqualTo(newRegion.getName());
            assertThat(response.name()).isEqualTo("치킨집");
            assertThat(response.phone()).isEqualTo("02-1111-2222");
            assertThat(response.description()).isEqualTo("바삭한 치킨");
            assertThat(response.address()).isEqualTo("서울시 강남구");
            assertThat(response.addressDetail()).isEqualTo("2층");
            assertThat(response.zipCode()).isEqualTo("12345");
            assertThat(response.minOrderAmount()).isEqualTo(15000);
            assertThat(response.deliveryFee()).isEqualTo(3500);
            assertThat(response.latitude()).isEqualByComparingTo("37.7654321");
            assertThat(response.longitude()).isEqualByComparingTo("127.7654321");
            assertThat(response.deliveryRadiusKm()).isEqualByComparingTo("4.5");
        }

        @Test
        @DisplayName("모든 수정 값이 null이면 기존 정보를 유지한다")
        void successAllNullRequestPreservesRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID ownerId = restaurant.getUser().getId();
            RestaurantResponse originalResponse = RestaurantResponse.from(restaurant);
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
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
                    null,
                    null
            );

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(restaurant.getUser()));

            RestaurantResponse response = restaurantService.updateRestaurant(restaurantId, request, ownerId);

            assertThat(response).isEqualTo(originalResponse);
            verifyNoInteractions(restaurantCategoryRepository, regionRepository);
        }

        @Test
        @DisplayName("MANAGER는 모든 음식점 정보를 수정할 수 있다")
        void successManagerUpdatesAnyRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            RestaurantCategory category = restaurantCategory("피자");
            Restaurant restaurant = restaurant(category, "피자집");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    null,
                    null,
                    "관리자 수정 피자집",
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
            UUID managerId = UUID.randomUUID();
            User manager = user(Role.MANAGER);
            ReflectionTestUtils.setField(manager, "id", managerId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(managerId)).thenReturn(Optional.of(manager));

            RestaurantResponse response = restaurantService.updateRestaurant(
                    restaurantId,
                    request,
                    managerId
            );

            assertThat(response.category()).isEqualTo("피자");
            assertThat(response.name()).isEqualTo("관리자 수정 피자집");
            assertThat(response.phone()).isEqualTo("02-000-0000");
            verifyNoInteractions(restaurantCategoryRepository, regionRepository);
        }

        @Test
        @DisplayName("MASTER는 다른 사용자의 음식점 정보를 수정할 수 있다")
        void successMasterUpdatesAnyRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("피자"), "피자집");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    null,
                    null,
                    "마스터 수정 피자집",
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
            UUID masterId = UUID.randomUUID();
            User master = user(Role.MASTER);
            ReflectionTestUtils.setField(master, "id", masterId);
            assertThat(masterId).isNotEqualTo(restaurant.getUser().getId());

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));

            RestaurantResponse response = restaurantService.updateRestaurant(restaurantId, request, masterId);

            assertThat(response.name()).isEqualTo("마스터 수정 피자집");
            assertThat(response.ownerUserId()).isEqualTo(restaurant.getUser().getId());
            verifyNoInteractions(restaurantCategoryRepository, regionRepository);
        }

        @Test
        @DisplayName("OWNER는 다른 사용자의 음식점을 수정할 수 없다")
        void failOwnerUpdatesOthersRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    "치킨",
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
            UUID ownerId = UUID.randomUUID();
            User owner = user(Role.OWNER);
            ReflectionTestUtils.setField(owner, "id", ownerId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> restaurantService.updateRestaurant(
                    restaurantId,
                    request,
                    ownerId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            assertThat(restaurant.getCategory().getCode()).isEqualTo("한식");
            verifyNoInteractions(restaurantCategoryRepository, regionRepository);
        }

        @Test
        @DisplayName("수정 대상 음식점이 존재하지 않으면 수정할 수 없다")
        void failRestaurantNotFound() {
            UUID restaurantId = UUID.randomUUID();
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    null,
                    null,
                    "수정",
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
            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.updateRestaurant(
                    restaurantId,
                    request,
                    UUID.randomUUID()
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_NOT_FOUND);

            verifyNoInteractions(restaurantCategoryRepository, regionRepository);
        }

        @Test
        @DisplayName("수정 요청 사용자가 존재하지 않으면 수정할 수 없다")
        void failUserNotFound() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID userId = UUID.randomUUID();
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    "치킨",
                    null,
                    "치킨집",
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

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.updateRestaurant(restaurantId, request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.USER_NOT_FOUND);

            assertThat(restaurant.getCategory().getCode()).isEqualTo("한식");
            assertThat(restaurant.getName()).isEqualTo("한식당");
            verifyNoInteractions(restaurantCategoryRepository, regionRepository);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리로 수정할 수 없다")
        void failRestaurantCategoryNotFound() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    "UNKNOWN",
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
            UUID masterId = UUID.randomUUID();
            User master = user(Role.MASTER);
            ReflectionTestUtils.setField(master, "id", masterId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
            when(restaurantCategoryRepository.findByCodeAndDeletedAtIsNull("UNKNOWN"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.updateRestaurant(
                    restaurantId,
                    request,
                    masterId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_CATEGORY_NOT_FOUND);
        }

        @Test
        @DisplayName("DB 권한이 허용된 역할이 아니면 수정할 수 없다")
        void failWhenTokenRoleIsPrivilegedButDbRoleIsNotAllowed() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID userId = UUID.randomUUID();
            User customer = user(Role.CUSTOMER);
            ReflectionTestUtils.setField(customer, "id", userId);
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    null,
                    null,
                    "수정 이름",
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

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> restaurantService.updateRestaurant(
                    restaurantId,
                    request,
                    userId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            assertThat(restaurant.getName()).isEqualTo("한식당");
        }

        @Test
        @DisplayName("DB 권한이 OWNER가 아니면 본인 음식점이어도 수정할 수 없다")
        void failWhenDbRoleIsNotOwnerEvenIfUserOwnsRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID ownerId = restaurant.getUser().getId();
            User customer = user(Role.CUSTOMER);
            ReflectionTestUtils.setField(customer, "id", ownerId);
            RestaurantUpdateRequest request = new RestaurantUpdateRequest(
                    null,
                    null,
                    "수정 이름",
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

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> restaurantService.updateRestaurant(
                    restaurantId,
                    request,
                    ownerId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            assertThat(restaurant.getName()).isEqualTo("한식당");
        }
    }

    @Nested
    @DisplayName("음식점 상태 변경")
    class ChangeRestaurantStatus {

        @Test
        @DisplayName("OWNER는 본인 음식점 상태를 변경할 수 있다")
        void successOwnerChangesOwnRestaurantStatus() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID ownerId = restaurant.getUser().getId();

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(restaurant.getUser()));

            RestaurantResponse response = restaurantService.changeRestaurantStatus(
                    restaurantId,
                    RestaurantStatus.CLOSED,
                    ownerId
            );

            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.CLOSED);
            assertThat(response.status()).isEqualTo(RestaurantStatus.CLOSED);
        }

        @Test
        @DisplayName("MANAGER는 모든 음식점 상태를 변경할 수 있다")
        void successManagerChangesAnyRestaurantStatus() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("치킨"), "치킨집");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID managerId = UUID.randomUUID();
            User manager = user(Role.MANAGER);
            ReflectionTestUtils.setField(manager, "id", managerId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(managerId)).thenReturn(Optional.of(manager));

            RestaurantResponse response = restaurantService.changeRestaurantStatus(
                    restaurantId,
                    RestaurantStatus.CLOSED,
                    managerId
            );

            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.CLOSED);
            assertThat(response.status()).isEqualTo(RestaurantStatus.CLOSED);
        }

        @Test
        @DisplayName("MASTER는 모든 음식점 상태를 변경할 수 있다")
        void successMasterChangesAnyRestaurantStatus() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("피자"), "피자집");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID masterId = UUID.randomUUID();
            User master = user(Role.MASTER);
            ReflectionTestUtils.setField(master, "id", masterId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));

            RestaurantResponse response = restaurantService.changeRestaurantStatus(
                    restaurantId,
                    RestaurantStatus.OPEN,
                    masterId
            );

            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.OPEN);
            assertThat(response.status()).isEqualTo(RestaurantStatus.OPEN);
        }

        @Test
        @DisplayName("상태 변경 대상 음식점이 존재하지 않으면 변경할 수 없다")
        void failRestaurantNotFound() {
            UUID restaurantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.changeRestaurantStatus(
                    restaurantId,
                    RestaurantStatus.CLOSED,
                    userId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_NOT_FOUND);

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("OWNER는 다른 사용자의 음식점 상태를 변경할 수 없다")
        void failOwnerChangesOthersRestaurantStatus() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID otherOwnerId = UUID.randomUUID();
            User otherOwner = user(Role.OWNER);
            ReflectionTestUtils.setField(otherOwner, "id", otherOwnerId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(otherOwnerId)).thenReturn(Optional.of(otherOwner));

            assertThatThrownBy(() -> restaurantService.changeRestaurantStatus(
                    restaurantId,
                    RestaurantStatus.CLOSED,
                    otherOwnerId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.OPEN);
        }

        @Test
        @DisplayName("상태 변경 요청 사용자가 존재하지 않으면 변경할 수 없다")
        void failUserNotFound() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID userId = UUID.randomUUID();

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.changeRestaurantStatus(
                    restaurantId,
                    RestaurantStatus.CLOSED,
                    userId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.USER_NOT_FOUND);

            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.OPEN);
        }

        @Test
        @DisplayName("DB 권한이 OWNER, MANAGER, MASTER가 아니면 본인 음식점이어도 상태를 변경할 수 없다")
        void failWhenDbRoleIsNotAllowedEvenIfUserOwnsRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID ownerId = restaurant.getUser().getId();
            User customer = user(Role.CUSTOMER);
            ReflectionTestUtils.setField(customer, "id", ownerId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> restaurantService.changeRestaurantStatus(
                    restaurantId,
                    RestaurantStatus.CLOSED,
                    ownerId
            ))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("음식점 삭제")
    class DeleteRestaurant {

        @Test
        @DisplayName("OWNER는 본인 음식점을 삭제할 수 있다")
        void successOwnerDeletesOwnRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID ownerId = restaurant.getUser().getId();

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(restaurant.getUser()));

            restaurantService.deleteRestaurant(restaurantId, ownerId);

            assertThat(restaurant.getDeletedBy()).isEqualTo(ownerId);
            assertThat(restaurant.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("MANAGER는 모든 음식점을 삭제할 수 있다")
        void successManagerDeletesAnyRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("치킨"), "치킨집");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID managerId = UUID.randomUUID();
            User manager = user(Role.MANAGER);
            ReflectionTestUtils.setField(manager, "id", managerId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(managerId)).thenReturn(Optional.of(manager));

            restaurantService.deleteRestaurant(restaurantId, managerId);

            assertThat(restaurant.getDeletedBy()).isEqualTo(managerId);
            assertThat(restaurant.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("MASTER는 모든 음식점을 삭제할 수 있다")
        void successMasterDeletesAnyRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("피자"), "피자집");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID masterId = UUID.randomUUID();
            User master = user(Role.MASTER);
            ReflectionTestUtils.setField(master, "id", masterId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));

            restaurantService.deleteRestaurant(restaurantId, masterId);

            assertThat(restaurant.getDeletedBy()).isEqualTo(masterId);
            assertThat(restaurant.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("삭제 대상 음식점이 존재하지 않거나 이미 삭제됐으면 삭제할 수 없다")
        void failRestaurantNotFound() {
            UUID restaurantId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.deleteRestaurant(restaurantId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.RESTAURANT_NOT_FOUND);

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("OWNER는 다른 사용자의 음식점을 삭제할 수 없다")
        void failOwnerDeletesOthersRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID otherOwnerId = UUID.randomUUID();
            User otherOwner = user(Role.OWNER);
            ReflectionTestUtils.setField(otherOwner, "id", otherOwnerId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(otherOwnerId)).thenReturn(Optional.of(otherOwner));

            assertThatThrownBy(() -> restaurantService.deleteRestaurant(restaurantId, otherOwnerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            assertThat(restaurant.getDeletedAt()).isNull();
            assertThat(restaurant.getDeletedBy()).isNull();
        }

        @Test
        @DisplayName("삭제 요청 사용자가 존재하지 않으면 삭제할 수 없다")
        void failUserNotFound() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID userId = UUID.randomUUID();

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.deleteRestaurant(restaurantId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.USER_NOT_FOUND);

            assertThat(restaurant.getDeletedAt()).isNull();
            assertThat(restaurant.getDeletedBy()).isNull();
        }

        @Test
        @DisplayName("DB 권한이 OWNER, MANAGER, MASTER가 아니면 본인 음식점이어도 삭제할 수 없다")
        void failWhenDbRoleIsNotAllowedEvenIfUserOwnsRestaurant() {
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = restaurant(restaurantCategory("한식"), "한식당");
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);
            UUID ownerId = restaurant.getUser().getId();
            User customer = user(Role.CUSTOMER);
            ReflectionTestUtils.setField(customer, "id", ownerId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(userRepository.findByIdAndDeletedAtIsNull(ownerId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> restaurantService.deleteRestaurant(restaurantId, ownerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            assertThat(restaurant.getDeletedAt()).isNull();
            assertThat(restaurant.getDeletedBy()).isNull();
        }
    }

    private static RestaurantCreateRequest validRestaurantCreateRequest(UUID regionId) {
        return new RestaurantCreateRequest(
                "한식",
                regionId,
                "한식당",
                "02-1234-5678",
                "정성껏 만든 한식",
                "서울시 강남구",
                "2층",
                "12345",
                15000,
                3500,
                new BigDecimal("37.1234567"),
                new BigDecimal("127.1234567"),
                new BigDecimal("4.5")
        );
    }

    private static Region leafRegion(String name) {
        Region region = Region.builder()
                .parent(null)
                .name(name)
                .build();
        ReflectionTestUtils.setField(region, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(region, "depth", Region.MAX_DEPTH);
        return region;
    }

    private static Region nonLeafRegion(String name) {
        Region region = Region.builder()
                .parent(null)
                .name(name)
                .build();
        ReflectionTestUtils.setField(region, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(region, "depth", 2);
        return region;
    }

    private static RestaurantCategory restaurantCategory(String code) {
        try {
            Constructor<RestaurantCategory> constructor = RestaurantCategory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            RestaurantCategory category = constructor.newInstance();
            ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(category, "code", code);
            return category;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("RestaurantCategory 테스트 객체 생성에 실패했습니다.", e);
        }
    }

    private static Restaurant restaurant(RestaurantCategory category, String name) {
        User owner = user(Role.OWNER);
        ReflectionTestUtils.setField(owner, "id", UUID.randomUUID());

        Restaurant restaurant = Restaurant.builder()
                .user(owner)
                .category(category)
                .region(leafRegion("역삼동"))
                .name(name)
                .phone("02-000-0000")
                .description("설명")
                .address("서울시")
                .addressDetail("1층")
                .zipCode("00000")
                .minOrderAmount(10000)
                .deliveryFee(3000)
                .status(RestaurantStatus.OPEN)
                .latitude(new BigDecimal("37.1234567"))
                .longitude(new BigDecimal("127.1234567"))
                .deliveryRadiusKm(new BigDecimal("3.5"))
                .build();
        ReflectionTestUtils.setField(restaurant, "id", UUID.randomUUID());
        return restaurant;
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
