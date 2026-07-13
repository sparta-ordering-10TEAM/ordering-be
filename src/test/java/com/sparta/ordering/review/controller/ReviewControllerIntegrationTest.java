package com.sparta.ordering.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.global.security.JwtTokenProvider;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderItem;
import com.sparta.ordering.order.entity.OrderStatus;
import com.sparta.ordering.order.repository.OrderRepository;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.entity.RestaurantCategory;
import com.sparta.ordering.restaurant.repository.RestaurantCategoryRepository;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
import com.sparta.ordering.review.dto.PostReviewRequest;
import com.sparta.ordering.review.dto.UpdateReviewRequest;
import com.sparta.ordering.review.entity.Review;
import com.sparta.ordering.review.repository.ReviewRepository;
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
import java.util.Optional;
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
class ReviewControllerIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // 사전 정의할 테스트 데이터 셋
    private User customer;
    private User manager;
    private Restaurant restaurant;
    private Product product;
    private Order order;

    private String customerToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 유저 생성 (고객, 사장, 어드민)
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

        manager = User.builder()
                .userName("manager_" + UUID.randomUUID())
                .nickName("manager_nick_" + UUID.randomUUID())
                .email("manager_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-5555-6666")
                .role(Role.MANAGER)
                .password("password")
                .build();
        userRepository.save(manager);

        // 가게 카테고리 생성 (생성자가 protected이므로 Reflection을 사용해 강제 생성)
        Constructor<RestaurantCategory> categoryConstructor = RestaurantCategory.class.getDeclaredConstructor();
        categoryConstructor.setAccessible(true);
        RestaurantCategory category = categoryConstructor.newInstance();
        ReflectionTestUtils.setField(category, "code", "CAT_" + UUID.randomUUID().toString().substring(0, 8));
        restaurantCategoryRepository.save(category);

        // 가게 생성
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
                .status(com.sparta.ordering.restaurant.entity.RestaurantStatus.OPEN)
                .latitude(new java.math.BigDecimal("37.123456"))
                .longitude(new java.math.BigDecimal("127.123456"))
                .deliveryRadiusKm(new java.math.BigDecimal("3.0"))
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

        // 주문 및 주문 상품(OrderItem) 연동
        order = Order.create("ORD_" + UUID.randomUUID().toString().substring(0, 8), restaurant, customer, "서울시 강남구", "맛있게 해주세요");
        OrderItem orderItem = OrderItem.create(product, 1);
        order.addOrderItem(orderItem);

        // 기본 주문 상태는 REQUESTED이나, 리뷰 작성은 COMPLETED 상태여야 하므로 리플렉션으로 상태 강제 주입
        ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.COMPLETED);
        orderRepository.save(order);

        // 테스트 계정별 JWT Access Token 생성
        customerToken = jwtTokenProvider.generateAccessToken(customer.getId(), customer.getUserName(), customer.getRole());
        managerToken = jwtTokenProvider.generateAccessToken(manager.getId(), manager.getUserName(), manager.getRole());
    }

    @Nested
    @DisplayName("리뷰 작성 (POST /api/orders/{orderId}/reviews)")
    class PostReview {

        @Test
        @DisplayName("성공 - CUSTOMER 권한으로 완료된 주문에 대해 유효한 리뷰를 작성")
        void success() throws Exception {
            // given
            PostReviewRequest request = new PostReviewRequest(5, "맛있어요!");

            // when & then
            mockMvc.perform(post("/api/orders/{orderId}/reviews", order.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201));

            // 실제 DB에 데이터가 적재되어 유효한지 영속성 검증
            boolean reviewExists = reviewRepository.existsByOrder_IdAndCustomer_IdAndDeletedAtIsNull(order.getId(), customer.getId());
            assertThat(reviewExists).isTrue();
        }

        @Test
        @DisplayName("실패 - 평점 유효성 검사 실패 (5 초과)")
        void failInvalidRating() throws Exception {
            // given
            PostReviewRequest request = new PostReviewRequest(6, "맛있어요!");

            // when & then
            mockMvc.perform(post("/api/orders/{orderId}/reviews", order.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("유효하지 않은 요청입니다."));
        }

        @Test
        @DisplayName("실패 - 평점 유효성 검사 실패 (1 미만)")
        void failInvalidRatingUnder() throws Exception {
            // given
            PostReviewRequest request = new PostReviewRequest(0, "맛있어요!");

            // when & then
            mockMvc.perform(post("/api/orders/{orderId}/reviews", order.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("유효하지 않은 요청입니다."));
        }

        @Test
        @DisplayName("실패 - 주문 완료 상태가 아닐 때 리뷰 작성 에러")
        void failOrderNotCompleted() throws Exception {
            // given (아직 완료되지 않은 주문 생성)
            Order uncompletedOrder = Order.create("ORD_" + UUID.randomUUID().toString().substring(0, 8), restaurant, customer, "서울시 강남구", "요청");
            OrderItem orderItem = OrderItem.create(product, 1);
            uncompletedOrder.addOrderItem(orderItem);
            orderRepository.save(uncompletedOrder);

            PostReviewRequest request = new PostReviewRequest(5, "맛있어요!");

            // when & then (주문 상태 미완료에 따른 거부 응답 검증)
            mockMvc.perform(post("/api/orders/{orderId}/reviews", uncompletedOrder.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("주문이 아직 완료되지 않았습니다."));
        }

        @Test
        @DisplayName("실패 - 이미 리뷰를 작성한 주문에 대해 중복 작성 에러")
        void failAlreadyReviewed() throws Exception {
            // given (기존에 작성해 둔 리뷰 1개 영속화)
            Review existingReview = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(5)
                    .comment("첫 리뷰")
                    .build();
            reviewRepository.save(existingReview);

            PostReviewRequest request = new PostReviewRequest(4, "두번째 리뷰");

            // when & then (동일 주문 중복 작성 시 CONFLICT 409 검증)
            mockMvc.perform(post("/api/orders/{orderId}/reviews", order.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("이미 리뷰를 작성한 주문입니다."));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문 ID로 리뷰 등록 시 에러")
        void failOrderNotFound() throws Exception {
            // given
            UUID nonExistentOrderId = UUID.randomUUID();
            PostReviewRequest request = new PostReviewRequest(5, "맛있어요!");

            // when & then
            mockMvc.perform(post("/api/orders/{orderId}/reviews", nonExistentOrderId)
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 주문을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - 타인의 주문 ID로 리뷰 등록 시 에러")
        void failOrderOfAnotherUser() throws Exception {
            // given
            User otherCustomer = User.builder()
                    .userName("other_" + UUID.randomUUID())
                    .nickName("other_nick_" + UUID.randomUUID())
                    .email("other_" + UUID.randomUUID() + "@example.com")
                    .phoneNumber("010-9999-9999")
                    .role(Role.CUSTOMER)
                    .password("password")
                    .build();
            userRepository.save(otherCustomer);

            Order otherOrder = Order.create("ORD_" + UUID.randomUUID().toString().substring(0, 8), restaurant, otherCustomer, "서울시 강남구", "요청");
            OrderItem orderItem = OrderItem.create(product, 1);
            otherOrder.addOrderItem(orderItem);
            ReflectionTestUtils.setField(otherOrder, "orderStatus", OrderStatus.COMPLETED);
            orderRepository.save(otherOrder);

            PostReviewRequest request = new PostReviewRequest(5, "맛있어요!");

            // when & then
            mockMvc.perform(post("/api/orders/{orderId}/reviews", otherOrder.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 주문을 찾을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("가게 리뷰 조회 (GET /api/restaurants/{restaurantId}/reviews)")
    class SearchRestaurantReviews {

        @Test
        @DisplayName("성공 - 인증 없이 가게 리뷰 조회 가능 및 페이징 정상 작동")
        void success() throws Exception {
            // given (가게 리뷰 등록)
            Review review = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(5)
                    .comment("최고의 치킨!")
                    .build();
            reviewRepository.save(review);

            // when & then
            mockMvc.perform(get("/api/restaurants/{restaurantId}/reviews", restaurant.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.content[0].comment").value("최고의 치킨!"))
                    .andExpect(jsonPath("$.data.content[0].rating").value(5));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 가게 ID로 리뷰 조회 시 에러")
        void failRestaurantNotFound() throws Exception {
            // given
            UUID nonExistentRestaurantId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/restaurants/{restaurantId}/reviews", nonExistentRestaurantId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 가게를 찾을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("상품 리뷰 조회 (GET /api/products/{productId}/reviews)")
    class SearchProductReviews {

        @Test
        @DisplayName("성공 - 인증 없이 상품 리뷰 조회 가능")
        void success() throws Exception {
            // given (리뷰 저장)
            Review review = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(4)
                    .comment("치킨 바삭해요")
                    .build();
            reviewRepository.save(review);

            // when & then
            mockMvc.perform(get("/api/products/{productId}/reviews", product.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.content[0].comment").value("치킨 바삭해요"))
                    .andExpect(jsonPath("$.data.content[0].rating").value(4));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품 ID로 리뷰 조회 시 에러")
        void failProductNotFound() throws Exception {
            // given
            UUID nonExistentProductId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/products/{productId}/reviews", nonExistentProductId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 상품을 찾을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("가게 상세 조회 - 평균 평점 검증 (GET /api/restaurants/{restaurantId})")
    class GetRestaurantDetailRating {

        @Test
        @DisplayName("성공 - 여러 리뷰 등록 시 가게 상세 조회 응답에 평균 평점 포함 확인")
        void success() throws Exception {
            // given
            // 1. 첫번째 완료 주문 리뷰 저장 (5점)
            Review review1 = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(5)
                    .comment("정말 맛있어요")
                    .build();
            reviewRepository.save(review1);

            // 2. 두번째 완료 주문 생성 및 리뷰 저장 (3점)
            Order order2 = Order.create("ORD_" + UUID.randomUUID().toString().substring(0, 8), restaurant, customer, "서울시 강남구", "요청");
            OrderItem orderItem2 = OrderItem.create(product, 1);
            order2.addOrderItem(orderItem2);
            ReflectionTestUtils.setField(order2, "orderStatus", OrderStatus.COMPLETED);
            orderRepository.save(order2);

            Review review2 = Review.builder()
                    .order(order2)
                    .customer(customer)
                    .rating(3)
                    .comment("그냥 그래요")
                    .build();
            reviewRepository.save(review2);

            // DB에서 수동 적재된 데이터를 바탕으로 평점을 명시적으로 계산/갱신
            restaurantRepository.updateAverageRating(restaurant.getId());

            // when & then (평균 평점 계산 검증: (5 + 3) / 2 = 4.0)
            mockMvc.perform(get("/api/restaurants/{restaurantId}", restaurant.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.averageRating").value(4.0));
        }
    }

    @Nested
    @DisplayName("리뷰 수정 (PATCH /api/reviews/{reviewId})")
    class UpdateReview {

        @Test
        @DisplayName("성공 - CUSTOMER 권한으로 본인의 리뷰 수정 및 DB 영속화 확인")
        void success() throws Exception {
            // given
            Review review = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(3)
                    .comment("기본 리뷰")
                    .build();
            reviewRepository.save(review);

            UpdateReviewRequest request = new UpdateReviewRequest(5, "수정된 맛있는 맛!");

            // when & then
            mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));

            // DB 검증
            Review updatedReview = reviewRepository.findById(review.getId()).orElseThrow();
            assertThat(updatedReview.getRating()).isEqualTo(5);
            assertThat(updatedReview.getComment()).isEqualTo("수정된 맛있는 맛!");
        }

        @Test
        @DisplayName("실패 - 평점 유효성 검사 실패 (1 미만)")
        void failInvalidRating() throws Exception {
            // given
            Review review = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(3)
                    .comment("기본 리뷰")
                    .build();
            reviewRepository.save(review);

            UpdateReviewRequest request = new UpdateReviewRequest(0, "수정");

            // when & then
            mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("실패 - 타인의 리뷰 수정 요청 시 권한 에러")
        void failForbidden() throws Exception {
            // given
            Review review = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(3)
                    .comment("기본 리뷰")
                    .build();
            reviewRepository.save(review);

            // 다른 임의의 고객 계정 정보와 토큰 생성
            User otherCustomer = User.builder()
                    .userName("other_" + UUID.randomUUID())
                    .nickName("other_nick_" + UUID.randomUUID())
                    .email("other_" + UUID.randomUUID() + "@example.com")
                    .phoneNumber("010-9999-9999")
                    .role(Role.CUSTOMER)
                    .password("password")
                    .build();
            userRepository.save(otherCustomer);
            String otherToken = jwtTokenProvider.generateAccessToken(otherCustomer.getId(), otherCustomer.getUserName(), otherCustomer.getRole());

            UpdateReviewRequest request = new UpdateReviewRequest(4, "수정 시도");

            // when & then (타인의 글 조회가 안 되므로 404 해당하는 리뷰를 찾을 수 없음 예외 발생 검증)
            mockMvc.perform(patch("/api/reviews/{reviewId}", review.getId())
                            .header("Authorization", "Bearer " + otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 리뷰를 찾을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 - CUSTOMER (DELETE /api/reviews/{reviewId})")
    class DeleteReview {

        @Test
        @DisplayName("성공 - CUSTOMER 권한으로 본인의 리뷰 삭제 및 soft delete 확인")
        void success() throws Exception {
            // given
            Review review = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(5)
                    .comment("리뷰 삭제 예정")
                    .build();
            reviewRepository.save(review);

            // when & then
            mockMvc.perform(delete("/api/reviews/{reviewId}", review.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));

            // DB 검증 (Soft Delete 컬럼 변경치 확인)
            Optional<Review> deletedReviewOpt = reviewRepository.findById(review.getId());
            assertThat(deletedReviewOpt).isPresent();
            Review deletedReview = deletedReviewOpt.get();
            assertThat(deletedReview.getDeletedAt()).isNotNull();
            assertThat(deletedReview.getDeletedBy()).isEqualTo(customer.getId());
        }

        @Test
        @DisplayName("실패 - 존재하지 않거나 타인의 리뷰 삭제 요청 시 에러")
        void failReviewNotFound() throws Exception {
            // given
            UUID nonExistentReviewId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/reviews/{reviewId}", nonExistentReviewId)
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 리뷰를 찾을 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("리뷰 삭제 - ADMIN (DELETE /api/admin/reviews/{reviewId})")
    class AdminDeleteReview {

        @Test
        @DisplayName("성공 - MANAGER 권한으로 리뷰 강제 soft delete")
        void success() throws Exception {
            // given
            Review review = Review.builder()
                    .order(order)
                    .customer(customer)
                    .rating(5)
                    .comment("어드민 삭제 예정")
                    .build();
            reviewRepository.save(review);

            // when & then (통합된 단일 DELETE /api/reviews/{reviewId} API에 MANAGER 권한 토큰 전달)
            mockMvc.perform(delete("/api/reviews/{reviewId}", review.getId())
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));

            // DB 검증
            Optional<Review> deletedReviewOpt = reviewRepository.findById(review.getId());
            assertThat(deletedReviewOpt).isPresent();
            Review deletedReview = deletedReviewOpt.get();
            assertThat(deletedReview.getDeletedAt()).isNotNull();
            assertThat(deletedReview.getDeletedBy()).isEqualTo(manager.getId());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 리뷰 강제 삭제 요청 시 에러")
        void failReviewNotFound() throws Exception {
            // given
            UUID nonExistentReviewId = UUID.randomUUID();

            // when & then
            mockMvc.perform(delete("/api/reviews/{reviewId}", nonExistentReviewId)
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 리뷰를 찾을 수 없습니다."));
        }
    }
}
