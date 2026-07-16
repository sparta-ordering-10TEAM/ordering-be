package com.sparta.ordering.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.product.dto.ProductCreateRequest;
import com.sparta.ordering.product.dto.ProductUpdateRequest;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantCategoryRepository restaurantCategoryRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtSessionService jwtSessionService;

    // 사전 정의할 테스트 데이터 셋
    private User owner;
    private User manager;
    private Restaurant restaurant;
    private Product product;

    private String ownerToken;
    private String managerToken;
    private String otherOwnerToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 유저 생성 (가게 사장, 매니저, 남의 가게 사장)
        owner = User.builder()
                .userName("owner_" + UUID.randomUUID())
                .nickName("owner_nick_" + UUID.randomUUID())
                .email("owner_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-3333-4444")
                .role(Role.OWNER)
                .password("password")
                .build();
        userRepository.save(owner);

        manager = User.builder()
                .userName("manager_" + UUID.randomUUID())
                .nickName("manager_nick_" + UUID.randomUUID())
                .email("manager_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-5555-6666")
                .role(Role.MANAGER)
                .password("password")
                .build();
        userRepository.save(manager);

        User otherOwner = User.builder()
                .userName("other_owner_" + UUID.randomUUID())
                .nickName("other_owner_nick_" + UUID.randomUUID())
                .email("other_owner_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-7777-8888")
                .role(Role.OWNER)
                .password("password")
                .build();
        userRepository.save(otherOwner);

        // 가게 카테고리 생성 (생성자가 protected이므로 Reflection을 사용해 강제 생성)
        Constructor<RestaurantCategory> categoryConstructor = RestaurantCategory.class.getDeclaredConstructor();
        categoryConstructor.setAccessible(true);
        RestaurantCategory category = categoryConstructor.newInstance();
        ReflectionTestUtils.setField(category, "code", "CAT_" + UUID.randomUUID().toString().substring(0, 8));
        restaurantCategoryRepository.save(category);

        // 지역 생성 (Restaurant.region_id NOT NULL 제약)
        Region sido = regionRepository.save(Region.builder().name("서울").build());
        Region sigungu = regionRepository.save(Region.builder().parent(sido).name("강남구").build());
        Region region = regionRepository.save(Region.builder().parent(sigungu).name("역삼동").build());

        // 가게 생성
        restaurant = Restaurant.builder()
                .user(owner)
                .category(category)
                .region(region)
                .name("맛있는 치킨집")
                .phone("02-123-4567")
                .description("치킨이 맛있는 집")
                .address("서울시 강남구")
                .addressDetail("101호")
                .zipCode("12345")
                .minOrderAmount(15000)
                .deliveryFee(3000)
                .status(RestaurantStatus.OPEN)
                .latitude(new BigDecimal("37.123456"))
                .longitude(new BigDecimal("127.123456"))
                .deliveryRadiusKm(new BigDecimal("3.0"))
                .build();
        restaurantRepository.save(restaurant);

        // 상품 생성
        product = Product.builder()
                .restaurant(restaurant)
                .name("후라이드 치킨")
                .description("바삭한 후라이드")
                .price(18000L)
                .build();
        productRepository.save(product);

        // 테스트 계정별 JWT Access Token 생성
        ownerToken = jwtSessionService.createJwtSession(owner.getId()).getAccessToken();
        managerToken = jwtSessionService.createJwtSession(manager.getId()).getAccessToken();
        otherOwnerToken = jwtSessionService.createJwtSession(otherOwner.getId()).getAccessToken();
    }

    @Nested
    @DisplayName("상품 생성 (POST /api/products)")
    class CreateProduct {

        @Test
        @DisplayName("성공 - OWNER 본인 가게에 상품 생성")
        void success() throws Exception {
            // given
            ProductCreateRequest request = new ProductCreateRequest(restaurant.getId(), "양념 치킨", "매콤달콤", 19000L);

            // when & then
            mockMvc.perform(post("/api/products")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.name").value("양념 치킨"));

            boolean exists = false;
            for (Product saved : productRepository.findAll()) {
                if (saved.getName().equals("양념 치킨")) {
                    exists = true;
                    break;
                }
            }
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("성공 - MANAGER는 타인 가게에도 상품 생성 가능")
        void successManager() throws Exception {
            // given
            ProductCreateRequest request = new ProductCreateRequest(restaurant.getId(), "간장 치킨", "짭짤한 맛", 20000L);

            // when & then
            mockMvc.perform(post("/api/products")
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201));
        }

        @Test
        @DisplayName("실패 - 타인 가게에 OWNER가 상품 생성 시도")
        void failForbidden() throws Exception {
            // given
            ProductCreateRequest request = new ProductCreateRequest(restaurant.getId(), "반반 치킨", "반반이 진리", 21000L);

            // when & then
            mockMvc.perform(post("/api/products")
                            .header("Authorization", "Bearer " + otherOwnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("인가되지 않은 요청입니다."));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 가게")
        void failRestaurantNotFound() throws Exception {
            // given
            UUID nonExistentRestaurantId = UUID.randomUUID();
            ProductCreateRequest request = new ProductCreateRequest(nonExistentRestaurantId, "허니 치킨", "달콤한 맛", 22000L);

            // when & then
            mockMvc.perform(post("/api/products")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 가게를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - 가격 유효성 검증 (0 이하)")
        void failInvalidPrice() throws Exception {
            // given
            ProductCreateRequest request = new ProductCreateRequest(restaurant.getId(), "이상한 치킨", "설명", 0L);

            // when & then
            mockMvc.perform(post("/api/products")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("상품 단건 조회 (GET /api/products/{productId})")
    class GetProduct {

        @Test
        @DisplayName("성공")
        void success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/products/{productId}", product.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.name").value("후라이드 치킨"))
                    .andExpect(jsonPath("$.data.price").value(18000));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void failNotFound() throws Exception {
            // given
            UUID nonExistentProductId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/products/{productId}", nonExistentProductId)
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 상품을 찾을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("상품 목록 조회 (GET /api/restaurants/{restaurantId}/products)")
    class GetProducts {

        @Test
        @DisplayName("성공 - 가게 상품 페이징 조회")
        void success() throws Exception {
            // when & then
            mockMvc.perform(get("/api/restaurants/{restaurantId}/products", restaurant.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.content[0].name").value("후라이드 치킨"));
        }

        @Test
        @DisplayName("성공 - 이름으로 검색 필터링")
        void successSearchByName() throws Exception {
            // given
            Product another = Product.builder()
                    .restaurant(restaurant)
                    .name("양념 치킨")
                    .description("매콤달콤")
                    .price(19000L)
                    .build();
            productRepository.save(another);

            // when & then
            mockMvc.perform(get("/api/restaurants/{restaurantId}/products", restaurant.getId())
                            .param("name", "양념")
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].name").value("양념 치킨"));
        }
    }

    @Nested
    @DisplayName("상품 수정 (PATCH /api/products/{productId})")
    class UpdateProduct {

        @Test
        @DisplayName("성공 - OWNER 본인 가게 상품 수정")
        void success() throws Exception {
            // given
            ProductUpdateRequest request = new ProductUpdateRequest("수정된 후라이드", "더 바삭해짐", 18500L);

            // when & then
            mockMvc.perform(patch("/api/products/{productId}", product.getId())
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.name").value("수정된 후라이드"));

            Product updated = productRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getName()).isEqualTo("수정된 후라이드");
            assertThat(updated.getPrice()).isEqualTo(18500L);
        }

        @Test
        @DisplayName("실패 - 타인 가게 상품 수정 시도")
        void failForbidden() throws Exception {
            // given
            ProductUpdateRequest request = new ProductUpdateRequest("해킹 시도", null, null);

            // when & then
            mockMvc.perform(patch("/api/products/{productId}", product.getId())
                            .header("Authorization", "Bearer " + otherOwnerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void failNotFound() throws Exception {
            // given
            UUID nonExistentProductId = UUID.randomUUID();
            ProductUpdateRequest request = new ProductUpdateRequest("수정", null, null);

            // when & then
            mockMvc.perform(patch("/api/products/{productId}", nonExistentProductId)
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("상품 삭제 (DELETE /api/products/{productId})")
    class DeleteProduct {

        @Test
        @DisplayName("성공 - OWNER 본인 가게 상품 soft delete")
        void success() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/products/{productId}", product.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));

            Product deleted = productRepository.findById(product.getId()).orElseThrow();
            assertThat(deleted.getDeletedAt()).isNotNull();
            assertThat(deleted.getDeletedBy()).isEqualTo(owner.getId());
        }

        @Test
        @DisplayName("성공 - MANAGER는 타인 가게 상품도 삭제 가능")
        void successManager() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/products/{productId}", product.getId())
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));

            Product deleted = productRepository.findById(product.getId()).orElseThrow();
            assertThat(deleted.getDeletedAt()).isNotNull();
            assertThat(deleted.getDeletedBy()).isEqualTo(manager.getId());
        }

        @Test
        @DisplayName("실패 - 타인 가게 상품 삭제 시도")
        void failForbidden() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/products/{productId}", product.getId())
                            .header("Authorization", "Bearer " + otherOwnerToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void failNotFound() throws Exception {
            // given
            UUID nonExistentProductId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/products/{productId}", nonExistentProductId)
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
