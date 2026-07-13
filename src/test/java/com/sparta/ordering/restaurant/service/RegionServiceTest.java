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
        @DisplayName("parentId가 없으면 최상위 지역을 조회한다")
        void getRootRegions() {
            Region seoul = region(null, "서울특별시");

            when(regionRepository.findByParentIsNullAndDeletedAtIsNull()).thenReturn(List.of(seoul));

            List<RegionResponse> result = regionService.getRegions(null);

            assertThat(result).containsExactly(RegionResponse.from(seoul));
        }

        @Test
        @DisplayName("parentId가 있으면 하위 지역을 조회한다")
        void getChildRegions() {
            Region seoul = region(null, "서울특별시");
            Region jongno = region(seoul, "종로구");

            when(regionRepository.findByParent_IdAndDeletedAtIsNull(seoul.getId())).thenReturn(List.of(jongno));

            List<RegionResponse> result = regionService.getRegions(seoul.getId());

            assertThat(result).containsExactly(RegionResponse.from(jongno));
            assertThat(result.get(0).parentId()).isEqualTo(seoul.getId());
            assertThat(result.get(0).depth()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("지역 생성")
    class CreateRegion {

        @Test
        @DisplayName("MANAGER는 최상위 지역을 생성할 수 있다")
        void successCreateRootRegion() {
            UUID managerId = adminUser(Role.MANAGER);
            RegionCreateRequest request = new RegionCreateRequest("서울특별시", null);

            when(regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull("서울특별시")).thenReturn(false);
            when(regionRepository.save(any(Region.class))).thenAnswer(invocation -> {
                Region saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
                return saved;
            });

            RegionResponse response = regionService.createRegion(request, managerId);

            ArgumentCaptor<Region> captor = ArgumentCaptor.forClass(Region.class);
            verify(regionRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("서울특별시");
            assertThat(captor.getValue().getDepth()).isEqualTo(1);
            assertThat(response.parentId()).isNull();
        }

        @Test
        @DisplayName("하위 지역 생성 시 depth는 부모+1로 계산된다")
        void successCreateChildRegionWithDepth() {
            UUID masterId = adminUser(Role.MASTER);
            Region seoul = region(null, "서울특별시");
            RegionCreateRequest request = new RegionCreateRequest("종로구", seoul.getId());

            when(regionRepository.findByIdAndDeletedAtIsNull(seoul.getId())).thenReturn(Optional.of(seoul));
            when(regionRepository.existsByParent_IdAndNameAndDeletedAtIsNull(seoul.getId(), "종로구"))
                    .thenReturn(false);
            when(regionRepository.save(any(Region.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RegionResponse response = regionService.createRegion(request, masterId);

            assertThat(response.depth()).isEqualTo(2);
            assertThat(response.parentId()).isEqualTo(seoul.getId());
        }

        @Test
        @DisplayName("최대 3단계를 초과하는 지역은 생성할 수 없다")
        void failDepthExceeded() {
            UUID managerId = adminUser(Role.MANAGER);
            Region seoul = region(null, "서울특별시");
            Region jongno = region(seoul, "종로구");
            Region sajik = region(jongno, "사직동");
            RegionCreateRequest request = new RegionCreateRequest("세종로", sajik.getId());

            when(regionRepository.findByIdAndDeletedAtIsNull(sajik.getId())).thenReturn(Optional.of(sajik));

            assertThatThrownBy(() -> regionService.createRegion(request, managerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_DEPTH_EXCEEDED);

            verify(regionRepository, never()).save(any(Region.class));
        }

        @Test
        @DisplayName("같은 부모 아래 중복된 이름의 지역은 생성할 수 없다")
        void failDuplicatedName() {
            UUID managerId = adminUser(Role.MANAGER);
            RegionCreateRequest request = new RegionCreateRequest("서울특별시", null);

            when(regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull("서울특별시")).thenReturn(true);

            assertThatThrownBy(() -> regionService.createRegion(request, managerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_ALREADY_EXISTS);

            verify(regionRepository, never()).save(any(Region.class));
        }

        @ParameterizedTest(name = "[{index}] role={0}")
        @EnumSource(value = Role.class, names = {"CUSTOMER", "OWNER"})
        @DisplayName("MANAGER, MASTER가 아닌 사용자는 지역을 생성할 수 없다")
        void failNotAdmin(Role role) {
            UUID userId = adminUser(role);
            RegionCreateRequest request = new RegionCreateRequest("서울특별시", null);

            assertThatThrownBy(() -> regionService.createRegion(request, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", AuthResponseCode.FORBIDDEN);

            verify(regionRepository, never()).save(any(Region.class));
        }
    }

    @Nested
    @DisplayName("지역 수정")
    class UpdateRegion {

        @Test
        @DisplayName("MANAGER는 지역 이름을 수정할 수 있다")
        void successRename() {
            UUID managerId = adminUser(Role.MANAGER);
            Region seoul = region(null, "서울특별시");

            when(regionRepository.findByIdAndDeletedAtIsNull(seoul.getId())).thenReturn(Optional.of(seoul));
            when(regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull("서울")).thenReturn(false);

            RegionResponse response = regionService.updateRegion(
                    seoul.getId(),
                    new RegionUpdateRequest("서울"),
                    managerId
            );

            assertThat(response.name()).isEqualTo("서울");
            assertThat(seoul.getName()).isEqualTo("서울");
        }

        @Test
        @DisplayName("존재하지 않는 지역은 수정할 수 없다")
        void failRegionNotFound() {
            UUID managerId = adminUser(Role.MANAGER);
            UUID regionId = UUID.randomUUID();

            when(regionRepository.findByIdAndDeletedAtIsNull(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> regionService.updateRegion(regionId, new RegionUpdateRequest("서울"), managerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_NOT_FOUND);
        }

        @Test
        @DisplayName("같은 부모 아래 이미 존재하는 이름으로 수정할 수 없다")
        void failDuplicatedName() {
            UUID managerId = adminUser(Role.MANAGER);
            Region seoul = region(null, "서울특별시");

            when(regionRepository.findByIdAndDeletedAtIsNull(seoul.getId())).thenReturn(Optional.of(seoul));
            when(regionRepository.existsByParentIsNullAndNameAndDeletedAtIsNull("부산광역시")).thenReturn(true);

            assertThatThrownBy(() ->
                    regionService.updateRegion(seoul.getId(), new RegionUpdateRequest("부산광역시"), managerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_ALREADY_EXISTS);

            assertThat(seoul.getName()).isEqualTo("서울특별시");
        }
    }

    @Nested
    @DisplayName("지역 삭제")
    class DeleteRegion {

        @Test
        @DisplayName("MASTER는 지역을 soft delete 할 수 있다")
        void successSoftDelete() {
            UUID masterId = adminUser(Role.MASTER);
            Region sajik = region(null, "사직동");

            when(regionRepository.findByIdAndDeletedAtIsNull(sajik.getId())).thenReturn(Optional.of(sajik));
            when(regionRepository.existsByParent_IdAndDeletedAtIsNull(sajik.getId())).thenReturn(false);
            when(restaurantRepository.existsByRegion_IdAndDeletedAtIsNull(sajik.getId())).thenReturn(false);

            regionService.deleteRegion(sajik.getId(), masterId);

            assertThat(sajik.getDeletedAt()).isNotNull();
            assertThat(sajik.getDeletedBy()).isEqualTo(masterId);
        }

        @Test
        @DisplayName("하위 지역이 있으면 삭제할 수 없다")
        void failHasChildren() {
            UUID managerId = adminUser(Role.MANAGER);
            Region seoul = region(null, "서울특별시");

            when(regionRepository.findByIdAndDeletedAtIsNull(seoul.getId())).thenReturn(Optional.of(seoul));
            when(regionRepository.existsByParent_IdAndDeletedAtIsNull(seoul.getId())).thenReturn(true);

            assertThatThrownBy(() -> regionService.deleteRegion(seoul.getId(), managerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_HAS_CHILDREN);

            assertThat(seoul.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("해당 지역에 등록된 음식점이 있으면 삭제할 수 없다")
        void failRegionInUse() {
            UUID managerId = adminUser(Role.MANAGER);
            Region sajik = region(null, "사직동");

            when(regionRepository.findByIdAndDeletedAtIsNull(sajik.getId())).thenReturn(Optional.of(sajik));
            when(regionRepository.existsByParent_IdAndDeletedAtIsNull(sajik.getId())).thenReturn(false);
            when(restaurantRepository.existsByRegion_IdAndDeletedAtIsNull(sajik.getId())).thenReturn(true);

            assertThatThrownBy(() -> regionService.deleteRegion(sajik.getId(), managerId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.REGION_IN_USE);

            assertThat(sajik.getDeletedAt()).isNull();
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

    private static Region region(Region parent, String name) {
        Region region = Region.builder().parent(parent).name(name).build();
        ReflectionTestUtils.setField(region, "id", UUID.randomUUID());
        return region;
    }
}
