package com.sparta.ordering.restaurant.service;

import com.sparta.ordering.global.code.AuthResponseCode;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.restaurant.dto.RegionCreateRequest;
import com.sparta.ordering.restaurant.dto.RegionResponse;
import com.sparta.ordering.restaurant.dto.RegionUpdateRequest;
import com.sparta.ordering.restaurant.entity.Region;
import com.sparta.ordering.restaurant.repository.RegionRepository;
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
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RegionService regionService;

    @Nested
    @DisplayName("지역 목록 조회")
    class GetRegions {

        @Test
        @DisplayName("parentId가 null이면 루트 지역을 조회한다")
        void successReturnsRootRegions() {
            Region seoul = region(null, "서울");
            when(regionRepository.findByParentIsNullAndDeletedAtIsNull()).thenReturn(List.of(seoul));

            List<RegionResponse> result = regionService.getRegions(null);

            assertThat(result).containsExactly(RegionResponse.from(seoul));
            verify(regionRepository).findByParentIsNullAndDeletedAtIsNull();
            verify(regionRepository, never()).findByParent_IdAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("parentId가 있으면 자식 지역을 조회한다")
        void successReturnsChildRegions() {
            UUID parentId = UUID.randomUUID();
            Region gangnam = region(null, "강남구");
            when(regionRepository.findByParent_IdAndDeletedAtIsNull(parentId)).thenReturn(List.of(gangnam));

            List<RegionResponse> result = regionService.getRegions(parentId);

            assertThat(result).containsExactly(RegionResponse.from(gangnam));
            verify(regionRepository).findByParent_IdAndDeletedAtIsNull(parentId);
            verify(regionRepository, never()).findByParentIsNullAndDeletedAtIsNull();
        }
    }

    @Nested
    @DisplayName("지역 생성")
    class CreateRegion {

        @Test
        @DisplayName("루트 지역을 생성할 수 있다")
        void successCreatesRootRegion() {
            UUID userId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            RegionCreateRequest request = new RegionCreateRequest("서울", null);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull("서울")).thenReturn(false);
            when(regionRepository.save(any(Region.class))).thenAnswer(invocation -> {
                Region saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", regionId);
                return saved;
            });

            RegionResponse response = regionService.createRegion(request, userId);

            ArgumentCaptor<Region> captor = ArgumentCaptor.forClass(Region.class);
            verify(regionRepository).save(captor.capture());
            assertThat(captor.getValue().getParent()).isNull();
            assertThat(captor.getValue().getName()).isEqualTo("서울");
            assertThat(captor.getValue().getDepth()).isEqualTo(1);
            assertThat(response.regionId()).isEqualTo(regionId);
            assertThat(response.parentId()).isNull();
            assertThat(response.name()).isEqualTo("서울");
        }

        @Test
        @DisplayName("자식 지역을 생성할 수 있다")
        void successCreatesChildRegion() {
            UUID userId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            User admin = user(Role.MANAGER);
            ReflectionTestUtils.setField(admin, "id", userId);
            Region parent = region(null, "서울");
            RegionCreateRequest request = new RegionCreateRequest("강남구", parent.getId());

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(parent.getId())).thenReturn(Optional.of(parent));
            when(regionRepository.existsByParent_IdAndNameAndDeletedAtIsNull(parent.getId(), "강남구"))
                    .thenReturn(false);
            when(regionRepository.save(any(Region.class))).thenAnswer(invocation -> {
                Region saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", regionId);
                return saved;
            });

            RegionResponse response = regionService.createRegion(request, userId);

            ArgumentCaptor<Region> captor = ArgumentCaptor.forClass(Region.class);
            verify(regionRepository).save(captor.capture());
            assertThat(captor.getValue().getParent()).isSameAs(parent);
            assertThat(captor.getValue().getName()).isEqualTo("강남구");
            assertThat(captor.getValue().getDepth()).isEqualTo(2);
            assertThat(response.regionId()).isEqualTo(regionId);
            assertThat(response.parentId()).isEqualTo(parent.getId());
        }

        @Test
        @DisplayName("형제 지역에 같은 이름이 있으면 생성할 수 없다")
        void failWhenSiblingNameDuplicated() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            Region parent = region(null, "서울");
            RegionCreateRequest request = new RegionCreateRequest("강남구", parent.getId());

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(parent.getId())).thenReturn(Optional.of(parent));
            when(regionRepository.existsByParent_IdAndNameAndDeletedAtIsNull(parent.getId(), "강남구"))
                    .thenReturn(true);

            assertThatThrownBy(() -> regionService.createRegion(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_ALREADY_EXISTS);

            verify(regionRepository, never()).save(any(Region.class));
        }

        @Test
        @DisplayName("부모 지역이 존재하지 않으면 생성할 수 없다")
        void failParentNotFound() {
            UUID userId = UUID.randomUUID();
            UUID parentId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            RegionCreateRequest request = new RegionCreateRequest("강남구", parentId);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(parentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> regionService.createRegion(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_NOT_FOUND);

            verify(regionRepository, never()).save(any(Region.class));
        }

        @ParameterizedTest(name = "[{index}] role={0}")
        @EnumSource(value = Role.class, names = {"CUSTOMER", "OWNER"})
        @DisplayName("admin이 아닌 사용자는 지역을 생성할 수 없다")
        void failWhenNotAdmin(Role role) {
            UUID userId = UUID.randomUUID();
            User nonAdmin = user(role);
            ReflectionTestUtils.setField(nonAdmin, "id", userId);
            RegionCreateRequest request = new RegionCreateRequest("서울", null);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(nonAdmin));

            assertThatThrownBy(() -> regionService.createRegion(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(regionRepository);
        }
    }

    @Nested
    @DisplayName("지역 수정")
    class UpdateRegion {

        @Test
        @DisplayName("admin은 지역 이름을 수정할 수 있다")
        void successAdminUpdatesRegion() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MANAGER);
            ReflectionTestUtils.setField(admin, "id", userId);
            Region region = region(null, "서울");
            RegionUpdateRequest request = new RegionUpdateRequest("부산");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(region.getId())).thenReturn(Optional.of(region));
            when(regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull("부산")).thenReturn(false);

            RegionResponse response = regionService.updateRegion(region.getId(), request, userId);

            assertThat(region.getName()).isEqualTo("부산");
            assertThat(response).isEqualTo(RegionResponse.from(region));
        }

        @Test
        @DisplayName("동일한 이름으로 수정하면 중복 검사 통과 후 성공한다")
        void successSameNameNoOp() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            Region region = region(null, "서울");
            RegionUpdateRequest request = new RegionUpdateRequest("서울");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(region.getId())).thenReturn(Optional.of(region));
            when(regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull("서울")).thenReturn(true);

            RegionResponse response = regionService.updateRegion(region.getId(), request, userId);

            assertThat(region.getName()).isEqualTo("서울");
            assertThat(response.name()).isEqualTo("서울");
        }

        @Test
        @DisplayName("형제 지역에 같은 이름이 있으면 수정할 수 없다")
        void failWhenSiblingNameDuplicated() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            Region parent = region(null, "서울");
            Region region = region(parent, "강남구");
            RegionUpdateRequest request = new RegionUpdateRequest("서초구");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(region.getId())).thenReturn(Optional.of(region));
            when(regionRepository.existsByParent_IdAndNameAndDeletedAtIsNull(parent.getId(), "서초구"))
                    .thenReturn(true);

            assertThatThrownBy(() -> regionService.updateRegion(region.getId(), request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_ALREADY_EXISTS);

            assertThat(region.getName()).isEqualTo("강남구");
        }

        @Test
        @DisplayName("존재하지 않는 지역은 수정할 수 없다")
        void failRegionNotFound() {
            UUID userId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            RegionUpdateRequest request = new RegionUpdateRequest("서울");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> regionService.updateRegion(regionId, request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_NOT_FOUND);
        }

        @Test
        @DisplayName("admin이 아니면 지역을 수정할 수 없다")
        void failWhenNotAdmin() {
            UUID userId = UUID.randomUUID();
            User owner = user(Role.OWNER);
            ReflectionTestUtils.setField(owner, "id", userId);
            RegionUpdateRequest request = new RegionUpdateRequest("서울");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> regionService.updateRegion(UUID.randomUUID(), request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(regionRepository);
        }
    }

    @Nested
    @DisplayName("지역 삭제")
    class DeleteRegion {

        @Test
        @DisplayName("admin은 자식/사용 중이 아닌 지역을 삭제할 수 있다")
        void successAdminDeletesRegion() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            Region region = region(null, "서울");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(region.getId())).thenReturn(Optional.of(region));
            when(regionRepository.existsByParent_IdAndDeletedAtIsNull(region.getId())).thenReturn(false);
            when(restaurantRepository.existsByRegion_IdAndDeletedAtIsNull(region.getId())).thenReturn(false);

            regionService.deleteRegion(region.getId(), userId);

            assertThat(region.getDeletedBy()).isEqualTo(userId);
            assertThat(region.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("자식 지역이 있으면 삭제할 수 없다")
        void failWhenHasChildren() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MANAGER);
            ReflectionTestUtils.setField(admin, "id", userId);
            Region region = region(null, "서울");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(region.getId())).thenReturn(Optional.of(region));
            when(regionRepository.existsByParent_IdAndDeletedAtIsNull(region.getId())).thenReturn(true);

            assertThatThrownBy(() -> regionService.deleteRegion(region.getId(), userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_HAS_CHILDREN);

            assertThat(region.getDeletedAt()).isNull();
            verifyNoInteractions(restaurantRepository);
        }

        @Test
        @DisplayName("음식점에서 사용 중인 지역은 삭제할 수 없다")
        void failWhenRegionInUse() {
            UUID userId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);
            Region region = region(null, "서울");

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(region.getId())).thenReturn(Optional.of(region));
            when(regionRepository.existsByParent_IdAndDeletedAtIsNull(region.getId())).thenReturn(false);
            when(restaurantRepository.existsByRegion_IdAndDeletedAtIsNull(region.getId())).thenReturn(true);

            assertThatThrownBy(() -> regionService.deleteRegion(region.getId(), userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_IN_USE);

            assertThat(region.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 지역은 삭제할 수 없다")
        void failRegionNotFound() {
            UUID userId = UUID.randomUUID();
            UUID regionId = UUID.randomUUID();
            User admin = user(Role.MASTER);
            ReflectionTestUtils.setField(admin, "id", userId);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(admin));
            when(regionRepository.findByIdAndDeletedAtIsNull(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> regionService.deleteRegion(regionId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_NOT_FOUND);

            verifyNoInteractions(restaurantRepository);
        }

        @Test
        @DisplayName("admin이 아니면 지역을 삭제할 수 없다")
        void failWhenNotAdmin() {
            UUID userId = UUID.randomUUID();
            User customer = user(Role.CUSTOMER);
            ReflectionTestUtils.setField(customer, "id", userId);

            when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> regionService.deleteRegion(UUID.randomUUID(), userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verifyNoInteractions(regionRepository, restaurantRepository);
        }
    }

    private static Region region(Region parent, String name) {
        Region region = Region.builder()
                .parent(parent)
                .name(name)
                .build();
        ReflectionTestUtils.setField(region, "id", UUID.randomUUID());
        return region;
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
