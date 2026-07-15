package com.sparta.ordering.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.cart.dto.CartItemQuantityRequest;
import com.sparta.ordering.cart.dto.CartItemRequest;
import com.sparta.ordering.cart.entity.Cart;
import com.sparta.ordering.cart.entity.CartItem;
import com.sparta.ordering.cart.repository.CartItemRepository;
import com.sparta.ordering.cart.repository.CartRepository;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.entity.RestaurantStatus;
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
@ActiveProfiles("test") // application-test.yml
class CartControllerIntegrationTest {

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
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private JwtSessionService jwtSessionService;

    // 사전 정의할 테스트 데이터 셋
    private User customer;
    private Restaurant restaurant;
    private Restaurant otherRestaurant;
    private Product product;
    private Product otherProduct;

    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 유저 생성
        customer = User.builder()
                .userName("customer_" + UUID.randomUUID())
                .nickName("customer_nick_" + UUID.randomUUID())
                .email("customer_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-1111-2222")
                .role(Role.CUSTOMER)
                .password("password")
                .build();
        userRepository.save(customer);

        User owner = User.builder()
                .userName("owner_" + UUID.randomUUID())
                .nickName("owner_nick_" + UUID.randomUUID())
                .email("owner_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-3333-4444")
                .role(Role.OWNER)
                .password("password")
                .build();
        userRepository.save(owner);

        // 가게 카테고리 생성 (생성자가 protected이므로 Reflection을 사용해 강제 생성)
        Constructor<RestaurantCategory> categoryConstructor = RestaurantCategory.class.getDeclaredConstructor();
        categoryConstructor.setAccessible(true);
        RestaurantCategory category = categoryConstructor.newInstance();
        ReflectionTestUtils.setField(category, "code", "CAT_" + UUID.randomUUID().toString().substring(0, 8));
        restaurantCategoryRepository.save(category);

        // 가게 2곳 생성 (다른 가게 상품 검증용)
        restaurant = Restaurant.builder()
                .user(owner)
                .category(category)
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

        otherRestaurant = Restaurant.builder()
                .user(owner)
                .category(category)
                .name("든든한 피자집")
                .phone("02-987-6543")
                .description("피자가 든든한 집")
                .address("서울시 서초구")
                .addressDetail("202호")
                .zipCode("54321")
                .minOrderAmount(20000)
                .deliveryFee(2500)
                .status(RestaurantStatus.OPEN)
                .latitude(new BigDecimal("37.223456"))
                .longitude(new BigDecimal("127.223456"))
                .deliveryRadiusKm(new BigDecimal("3.0"))
                .build();
        restaurantRepository.save(otherRestaurant);

        // 상품 생성
        product = Product.builder()
                .restaurant(restaurant)
                .name("후라이드 치킨")
                .description("바삭한 후라이드")
                .price(18000L)
                .build();
        productRepository.save(product);

        otherProduct = Product.builder()
                .restaurant(otherRestaurant)
                .name("페퍼로니 피자")
                .description("매콤한 페퍼로니")
                .price(22000L)
                .build();
        productRepository.save(otherProduct);

        // 테스트 계정 JWT Access Token 생성
        customerToken = jwtSessionService.createJwtSession(customer.getId()).getAccessToken();
    }

    @Nested
    @DisplayName("장바구니 상품 추가 (POST /api/cart/items)")
    class AddItem {

        @Test
        @DisplayName("성공 - 카트 최초 생성하며 상품 추가")
        void success() throws Exception {
            // given
            CartItemRequest request = new CartItemRequest(product.getId(), 2);

            // when & then
            mockMvc.perform(post("/api/cart/items")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.restaurantId").value(restaurant.getId().toString()))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(2));

            Cart cart = cartRepository.findByUser_IdAndDeletedAtIsNull(customer.getId()).orElseThrow();
            assertThat(cart.getRestaurant().getId()).isEqualTo(restaurant.getId());
        }

        @Test
        @DisplayName("성공 - 이미 담긴 상품이면 수량 증가")
        void successIncreaseQuantity() throws Exception {
            // given
            CartItemRequest request = new CartItemRequest(product.getId(), 2);
            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // when & then (같은 상품 다시 담기)
            mockMvc.perform(post("/api/cart/items")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(4));
        }

        @Test
        @DisplayName("실패 - 다른 가게 상품 추가 시도")
        void failDifferentRestaurant() throws Exception {
            // given (먼저 restaurant 상품 담기)
            CartItemRequest firstRequest = new CartItemRequest(product.getId(), 1);
            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)));

            CartItemRequest otherRequest = new CartItemRequest(otherProduct.getId(), 1);

            // when & then
            mockMvc.perform(post("/api/cart/items")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(otherRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("장바구니에는 같은 식당의 상품만 담을 수 있습니다."));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void failProductNotFound() throws Exception {
            // given
            UUID nonExistentProductId = UUID.randomUUID();
            CartItemRequest request = new CartItemRequest(nonExistentProductId, 1);

            // when & then
            mockMvc.perform(post("/api/cart/items")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 상품을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - 수량 유효성 검증 (99개 초과)")
        void failInvalidQuantity() throws Exception {
            // given
            CartItemRequest request = new CartItemRequest(product.getId(), 100);

            // when & then
            mockMvc.perform(post("/api/cart/items")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("성공 - 카트를 비운 뒤 다른 가게 상품 담기 (재생성 검증)")
        void successReAddAfterEmpty() throws Exception {
            // given (restaurant 상품 담고 -> 삭제해서 카트 비우기)
            CartItemRequest firstRequest = new CartItemRequest(product.getId(), 1);
            String firstResponseJson = mockMvc.perform(post("/api/cart/items")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andReturn().getResponse().getContentAsString();
            UUID firstCartId = cartRepository.findByUser_IdAndDeletedAtIsNull(customer.getId()).orElseThrow().getId();
            CartItem firstItem = cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(firstCartId).get(0);

            mockMvc.perform(delete("/api/cart/items/{cartItemId}", firstItem.getId())
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.restaurantId").doesNotExist());

            // when & then (이제 다른 가게 상품을 담아도 성공해야 함 - 새 카트 생성)
            CartItemRequest otherRequest = new CartItemRequest(otherProduct.getId(), 1);
            mockMvc.perform(post("/api/cart/items")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(otherRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.restaurantId").value(otherRestaurant.getId().toString()));

            Cart newCart = cartRepository.findByUser_IdAndDeletedAtIsNull(customer.getId()).orElseThrow();
            assertThat(newCart.getId()).isNotEqualTo(firstCartId);
        }
    }

    @Nested
    @DisplayName("내 카트 조회 (GET /api/cart)")
    class GetMyCart {

        @Test
        @DisplayName("성공 - 카트 있음")
        void successWithCart() throws Exception {
            // given
            CartItemRequest request = new CartItemRequest(product.getId(), 3);
            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // when & then
            mockMvc.perform(get("/api/cart")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].quantity").value(3));
        }

        @Test
        @DisplayName("성공 - 카트 없으면 빈 카트 반환")
        void successNoCart() throws Exception {
            // when & then
            mockMvc.perform(get("/api/cart")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cartId").doesNotExist())
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }
    }

    @Nested
    @DisplayName("카트 아이템 수량 수정 (PATCH /api/cart/items/{cartItemId})")
    class UpdateItemQuantity {

        @Test
        @DisplayName("성공")
        void success() throws Exception {
            // given
            CartItemRequest addRequest = new CartItemRequest(product.getId(), 1);
            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addRequest)));

            UUID cartId = cartRepository.findByUser_IdAndDeletedAtIsNull(customer.getId()).orElseThrow().getId();
            CartItem item = cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartId).get(0);

            CartItemQuantityRequest request = new CartItemQuantityRequest(5);

            // when & then
            mockMvc.perform(patch("/api/cart/items/{cartItemId}", item.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items[0].quantity").value(5));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 카트 아이템")
        void failNotFound() throws Exception {
            // given
            UUID nonExistentCartItemId = UUID.randomUUID();
            CartItemQuantityRequest request = new CartItemQuantityRequest(5);

            // when & then
            mockMvc.perform(patch("/api/cart/items/{cartItemId}", nonExistentCartItemId)
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("카트 아이템 삭제 (DELETE /api/cart/items/{cartItemId})")
    class RemoveItem {

        @Test
        @DisplayName("성공 - 남은 아이템 있으면 카트 유지")
        void successKeepsCart() throws Exception {
            // given (두 개 담기)
            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new CartItemRequest(product.getId(), 1))));

            Product secondProduct = Product.builder()
                    .restaurant(restaurant)
                    .name("치즈볼")
                    .description("고소한 치즈볼")
                    .price(5000L)
                    .build();
            productRepository.save(secondProduct);
            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new CartItemRequest(secondProduct.getId(), 1))));

            UUID cartId = cartRepository.findByUser_IdAndDeletedAtIsNull(customer.getId()).orElseThrow().getId();
            CartItem firstItem = cartItemRepository.findByCart_IdAndProduct_IdAndDeletedAtIsNull(cartId, product.getId()).orElseThrow();

            // when & then (하나만 삭제하면 카트는 유지)
            mockMvc.perform(delete("/api/cart/items/{cartItemId}", firstItem.getId())
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.restaurantId").value(restaurant.getId().toString()))
                    .andExpect(jsonPath("$.data.items.length()").value(1));
        }

        @Test
        @DisplayName("성공 - 마지막 아이템 삭제 시 빈 카트 반환")
        void successEmptiesCart() throws Exception {
            // given
            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new CartItemRequest(product.getId(), 1))));

            UUID cartId = cartRepository.findByUser_IdAndDeletedAtIsNull(customer.getId()).orElseThrow().getId();
            CartItem item = cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartId).get(0);

            // when & then
            mockMvc.perform(delete("/api/cart/items/{cartItemId}", item.getId())
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cartId").doesNotExist())
                    .andExpect(jsonPath("$.data.items").isEmpty());

            Cart deletedCart = cartRepository.findById(cartId).orElseThrow();
            assertThat(deletedCart.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 카트 아이템")
        void failNotFound() throws Exception {
            // given
            UUID nonExistentCartItemId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/cart/items/{cartItemId}", nonExistentCartItemId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("카트 비우기 (DELETE /api/cart)")
    class ClearCart {

        @Test
        @DisplayName("성공")
        void success() throws Exception {
            // given
            mockMvc.perform(post("/api/cart/items")
                    .header("Authorization", "Bearer " + customerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new CartItemRequest(product.getId(), 2))));

            UUID cartId = cartRepository.findByUser_IdAndDeletedAtIsNull(customer.getId()).orElseThrow().getId();

            // when & then
            mockMvc.perform(delete("/api/cart")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items").isEmpty());

            Cart deletedCart = cartRepository.findById(cartId).orElseThrow();
            assertThat(deletedCart.getDeletedAt()).isNotNull();
            assertThat(cartItemRepository.findByCart_IdAndDeletedAtIsNullWithProduct(cartId)).isEmpty();
        }

        @Test
        @DisplayName("실패 - 카트 없음")
        void failCartNotFound() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/cart")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당 장바구니를 찾을 수 없습니다."));
        }
    }
}
