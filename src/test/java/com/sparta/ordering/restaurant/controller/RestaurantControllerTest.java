package com.sparta.ordering.restaurant.controller;

import com.sparta.ordering.global.dto.GeneralResponse;
import com.sparta.ordering.restaurant.dto.RestaurantCreateRequest;
import com.sparta.ordering.restaurant.dto.RestaurantResponse;
import com.sparta.ordering.restaurant.dto.RestaurantUpdateRequest;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
import com.sparta.ordering.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerTest {

    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private RestaurantController restaurantController;

    @Test
    @DisplayName("음식점 컨트롤러는 /api 기본 경로로 매핑된다")
    void restaurantControllerBaseMapping() {
        RequestMapping requestMapping = RestaurantController.class.getAnnotation(RequestMapping.class);

        assertThat(requestMapping).isNotNull();
        assertThat(requestMapping.value()).containsExactly("/api");
    }

    @Test
    @DisplayName("GET /api/restaurants는 메서드 수준 권한 제한 없이 조회된다")
    void getRestaurantsMappingAndAuthorization() throws NoSuchMethodException {
        Method method = RestaurantController.class.getDeclaredMethod(
                "getRestaurants",
                String.class,
                Pageable.class
        );

        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/restaurants");
        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();

        RequestParam requestParam = parameterAnnotation(method, 0, RequestParam.class);
        assertThat(requestParam.required()).isFalse();
        parameterAnnotation(method, 1, PageableDefault.class);
    }

    @Test
    @DisplayName("GET /api/restaurants/{restaurantId}는 메서드 수준 권한 제한 없이 조회된다")
    void getRestaurantMappingAndAuthorization() throws NoSuchMethodException {
        Method method = RestaurantController.class.getDeclaredMethod(
                "getRestaurant",
                UUID.class
        );

        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/restaurants/{restaurantId}");
        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
        parameterAnnotation(method, 0, PathVariable.class);
    }

    @Test
    @DisplayName("GET /api/users/me/restaurants는 OWNER만 접근할 수 있다")
    void getOwnerRestaurantsMappingAndAuthorization() throws NoSuchMethodException {
        Method method = RestaurantController.class.getDeclaredMethod(
                "getOwnerRestaurants",
                UUID.class,
                Pageable.class
        );

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(getMapping).isNotNull();
        assertThat(getMapping.value()).containsExactly("/users/me/restaurants");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('OWNER')");
        parameterAnnotation(method, 0, AuthenticationPrincipal.class);
        parameterAnnotation(method, 1, PageableDefault.class);
    }

    @Test
    @DisplayName("POST /api/restaurants는 OWNER만 접근할 수 있다")
    void createRestaurantMappingAndAuthorization() throws NoSuchMethodException {
        Method method = RestaurantController.class.getDeclaredMethod(
                "createRestaurant",
                RestaurantCreateRequest.class,
                UUID.class
        );

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(postMapping).isNotNull();
        assertThat(postMapping.value()).containsExactly("/restaurants");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('OWNER')");
        parameterAnnotation(method, 0, Valid.class);
        parameterAnnotation(method, 0, RequestBody.class);
        parameterAnnotation(method, 1, AuthenticationPrincipal.class);
    }

    @Test
    @DisplayName("PATCH /api/restaurants/{restaurantId}는 OWNER, MANAGER, MASTER만 접근할 수 있다")
    void updateRestaurantMappingAndAuthorization() throws NoSuchMethodException {
        Method method = RestaurantController.class.getDeclaredMethod(
                "updateRestaurant",
                UUID.class,
                RestaurantUpdateRequest.class,
                UUID.class
        );

        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(patchMapping).isNotNull();
        assertThat(patchMapping.value()).containsExactly("/restaurants/{restaurantId}");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('MANAGER', 'MASTER', 'OWNER')");
        parameterAnnotation(method, 0, PathVariable.class);
        parameterAnnotation(method, 1, Valid.class);
        parameterAnnotation(method, 1, RequestBody.class);
        parameterAnnotation(method, 2, AuthenticationPrincipal.class);
    }

    @Test
    @DisplayName("DELETE /api/restaurants/{restaurantId}는 OWNER, MANAGER, MASTER만 접근할 수 있다")
    void deleteRestaurantMappingAndAuthorization() throws NoSuchMethodException {
        Method method = RestaurantController.class.getDeclaredMethod(
                "deleteRestaurant",
                UUID.class,
                UUID.class
        );

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(deleteMapping).isNotNull();
        assertThat(deleteMapping.value()).containsExactly("/restaurants/{restaurantId}");
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('MANAGER', 'MASTER', 'OWNER')");
        parameterAnnotation(method, 0, PathVariable.class);
        parameterAnnotation(method, 1, AuthenticationPrincipal.class);
    }

    @Test
    @DisplayName("음식점 목록 조회 요청은 카테고리와 페이징을 전달하고 성공 응답을 반환한다")
    void getRestaurantsReturnsOk() {
        String category = "한식";
        Pageable pageable = PageRequest.of(0, 20);
        RestaurantResponse restaurant = restaurantResponse(UUID.randomUUID());
        Page<RestaurantResponse> restaurants = new PageImpl<>(List.of(restaurant), pageable, 1);

        when(restaurantService.getRestaurants(category, pageable)).thenReturn(restaurants);

        ResponseEntity<GeneralResponse<Page<RestaurantResponse>>> response =
                restaurantController.getRestaurants(category, pageable);

        assertResponse(response, HttpStatus.OK, restaurants);
        verify(restaurantService).getRestaurants(category, pageable);
    }

    @Test
    @DisplayName("음식점 상세 조회 요청은 음식점 ID를 전달하고 성공 응답을 반환한다")
    void getRestaurantReturnsOk() {
        UUID restaurantId = UUID.randomUUID();
        RestaurantResponse restaurant = restaurantResponse(restaurantId);

        when(restaurantService.getRestaurant(restaurantId)).thenReturn(restaurant);

        ResponseEntity<GeneralResponse<RestaurantResponse>> response =
                restaurantController.getRestaurant(restaurantId);

        assertResponse(response, HttpStatus.OK, restaurant);
        verify(restaurantService).getRestaurant(restaurantId);
    }

    @Test
    @DisplayName("내 음식점 조회 요청은 사용자 ID와 페이징을 전달하고 성공 응답을 반환한다")
    void getOwnerRestaurantsReturnsOk() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        RestaurantResponse restaurant = restaurantResponse(UUID.randomUUID());
        Page<RestaurantResponse> restaurants = new PageImpl<>(List.of(restaurant), pageable, 1);

        when(restaurantService.getOwnerRestaurants(userId, pageable)).thenReturn(restaurants);

        ResponseEntity<GeneralResponse<Page<RestaurantResponse>>> response =
                restaurantController.getOwnerRestaurants(userId, pageable);

        assertResponse(response, HttpStatus.OK, restaurants);
        verify(restaurantService).getOwnerRestaurants(userId, pageable);
    }

    @Test
    @DisplayName("음식점 생성 요청은 요청 정보와 사용자 ID를 전달하고 생성 응답을 반환한다")
    void createRestaurantReturnsCreated() {
        UUID userId = UUID.randomUUID();
        RestaurantCreateRequest request = restaurantCreateRequest();
        RestaurantResponse restaurant = restaurantResponse(UUID.randomUUID());

        when(restaurantService.createRestaurant(request, userId)).thenReturn(restaurant);

        ResponseEntity<GeneralResponse<RestaurantResponse>> response =
                restaurantController.createRestaurant(request, userId);

        assertResponse(response, HttpStatus.CREATED, restaurant);
        verify(restaurantService).createRestaurant(request, userId);
    }

    @Test
    @DisplayName("음식점 수정 요청은 음식점 ID와 요청 정보, 사용자 ID를 전달하고 성공 응답을 반환한다")
    void updateRestaurantReturnsOk() {
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RestaurantUpdateRequest request = restaurantUpdateRequest();
        RestaurantResponse restaurant = restaurantResponse(restaurantId);

        when(restaurantService.updateRestaurant(restaurantId, request, userId)).thenReturn(restaurant);

        ResponseEntity<GeneralResponse<RestaurantResponse>> response =
                restaurantController.updateRestaurant(restaurantId, request, userId);

        assertResponse(response, HttpStatus.OK, restaurant);
        verify(restaurantService).updateRestaurant(restaurantId, request, userId);
    }

    @Test
    @DisplayName("음식점 삭제 요청은 서비스에 위임하고 성공 응답을 반환한다")
    void deleteRestaurantReturnsOk() {
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ResponseEntity<GeneralResponse<Void>> response = restaurantController.deleteRestaurant(restaurantId, userId);

        assertResponse(response, HttpStatus.OK, null);
        verify(restaurantService).deleteRestaurant(restaurantId, userId);
    }

    private static <T> void assertResponse(
            ResponseEntity<GeneralResponse<T>> response,
            HttpStatus expectedStatus,
            T expectedData
    ) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(expectedStatus.value());
        assertThat(response.getBody().getData()).isSameAs(expectedData);
    }

    private static <A extends Annotation> A parameterAnnotation(
            Method method,
            int parameterIndex,
            Class<A> annotationType
    ) {
        A annotation = method.getParameters()[parameterIndex].getAnnotation(annotationType);

        assertThat(annotation)
                .as("%s 메서드의 파라미터 인덱스 %d에 @%s가 선언되어야 한다",
                        method.getName(), parameterIndex, annotationType.getSimpleName())
                .isNotNull();
        return annotation;
    }

    private static RestaurantCreateRequest restaurantCreateRequest() {
        return new RestaurantCreateRequest(
                "한식",
                "테스트 음식점",
                "02-1234-5678",
                "테스트 음식점 설명",
                "서울시 중구 세종대로 1",
                "1층",
                "04524",
                10_000,
                3_000,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                new BigDecimal("3.0")
        );
    }

    private static RestaurantUpdateRequest restaurantUpdateRequest() {
        return new RestaurantUpdateRequest(
                null,
                "수정된 음식점",
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

    private static RestaurantResponse restaurantResponse(UUID restaurantId) {
        return new RestaurantResponse(
                restaurantId,
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "한식",
                "테스트 음식점",
                "02-1234-5678",
                "테스트 음식점 설명",
                "서울시 중구 세종대로 1",
                "1층",
                "04524",
                10_000,
                3_000,
                RestaurantStatus.CLOSED,
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                new BigDecimal("3.0")
        );
    }
}
