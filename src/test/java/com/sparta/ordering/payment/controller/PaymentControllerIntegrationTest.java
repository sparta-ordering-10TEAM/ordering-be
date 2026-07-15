package com.sparta.ordering.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.ordering.auth.security.session.JwtSessionService;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.entity.OrderItem;
import com.sparta.ordering.order.repository.OrderRepository;
import com.sparta.ordering.payment.dto.PaymentCancelRequest;
import com.sparta.ordering.payment.dto.PaymentRequest;
import com.sparta.ordering.payment.entity.Payment;
import com.sparta.ordering.payment.entity.PaymentStatus;
import com.sparta.ordering.payment.repository.PaymentRepository;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

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
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtSessionService jwtSessionService;

    @PersistenceContext
    private EntityManager entityManager;

    // 사전 정의할 테스트 데이터 셋
    private User customer;
    private User owner;
    private User manager;
    private Restaurant restaurant;
    private Product product;
    private Order order;

    private String customerToken;
    private String ownerToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 유저 생성 (고객, 사장, 매니저)
        customer = User.builder()
                .userName("customer_" + UUID.randomUUID())
                .nickName("customer_nick_" + UUID.randomUUID())
                .email("customer_" + UUID.randomUUID() + "@example.com")
                .phoneNumber("010-1111-2222")
                .role(Role.CUSTOMER)
                .password("password")
                .build();
        userRepository.save(customer);

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
                .status(RestaurantStatus.OPEN)
                .latitude(new BigDecimal("37.123456"))
                .longitude(new BigDecimal("127.123456"))
                .deliveryRadiusKm(new BigDecimal("3.0"))
                .build();
        restaurantRepository.save(restaurant);

        // 상품 생성 (mockConfirm이 100만원 이상이면 실패하므로 임계값 아래로 설정)
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
        orderRepository.save(order);

        // 테스트 계정별 JWT Access Token 생성
        customerToken = jwtSessionService.createJwtSession(customer.getId()).getAccessToken();
        ownerToken = jwtSessionService.createJwtSession(owner.getId()).getAccessToken();
        managerToken = jwtSessionService.createJwtSession(manager.getId()).getAccessToken();
    }

    private Payment saveDonePayment(Order targetOrder) {
        Payment payment = Payment.builder()
                .order(targetOrder)
                .amount(BigDecimal.valueOf(targetOrder.getTotalPrice()))
                .paymentKey("paymentKey_" + UUID.randomUUID())
                .build();
        payment.approve(Instant.now(), "신한카드");
        return paymentRepository.save(payment);
    }

    @Nested
    @DisplayName("결제 생성 (POST /api/payments)")
    class CreatePayment {

        @Test
        @DisplayName("성공 - CUSTOMER 권한으로 정상 금액 결제 생성")
        void success() throws Exception {
            // given
            PaymentRequest request = new PaymentRequest(order.getId(), "paymentKey_" + UUID.randomUUID(), BigDecimal.valueOf(order.getTotalPrice()));

            // when & then
            mockMvc.perform(post("/api/payments")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.status").value("DONE"));

            // 실제 DB에 결제가 DONE 상태로 적재되었는지 검증
            Payment saved = paymentRepository.findAll().stream()
                    .filter(p -> p.getOrder().getId().equals(order.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.DONE);
        }

        @Test
        @DisplayName("실패 - 주문 금액과 결제 금액 불일치")
        void failAmountInvalid() throws Exception {
            // given
            PaymentRequest request = new PaymentRequest(order.getId(), "paymentKey_" + UUID.randomUUID(), BigDecimal.valueOf(order.getTotalPrice() + 1000));

            // when & then
            mockMvc.perform(post("/api/payments")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("결제 금액이 주문 금액과 일치하지 않습니다."));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        void failOrderNotFound() throws Exception {
            // given
            UUID nonExistentOrderId = UUID.randomUUID();
            PaymentRequest request = new PaymentRequest(nonExistentOrderId, "paymentKey_" + UUID.randomUUID(), BigDecimal.valueOf(1000));

            // when & then
            mockMvc.perform(post("/api/payments")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 주문을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - PG 승인 거절 시 결제 ABORTED 처리")
        void failPgApprovalRejected() throws Exception {
            // given (mockConfirm은 100만원 이상이면 실패하는 임의 규칙)
            Order bigOrder = Order.create("ORD_" + UUID.randomUUID().toString().substring(0, 8), restaurant, customer, "서울시 강남구", "요청");
            OrderItem bigItem = OrderItem.create(product, 100); // 18000 * 100 = 1,800,000
            bigOrder.addOrderItem(bigItem);
            orderRepository.save(bigOrder);

            PaymentRequest request = new PaymentRequest(bigOrder.getId(), "paymentKey_" + UUID.randomUUID(), BigDecimal.valueOf(bigOrder.getTotalPrice()));

            // when & then
            mockMvc.perform(post("/api/payments")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502));

            Payment saved = paymentRepository.findAll().stream()
                    .filter(p -> p.getOrder().getId().equals(bigOrder.getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.ABORTED);
        }
    }

    @Nested
    @DisplayName("결제 단건 조회 (GET /api/payments/{paymentId})")
    class GetPayment {

        @Test
        @DisplayName("성공 - CUSTOMER 본인 결제 조회")
        void successCustomer() throws Exception {
            // given
            Payment payment = saveDonePayment(order);

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", payment.getId())
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.status").value("DONE"));
        }

        @Test
        @DisplayName("성공 - OWNER 자기 가게 결제 조회")
        void successOwner() throws Exception {
            // given
            Payment payment = saveDonePayment(order);

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", payment.getId())
                            .header("Authorization", "Bearer " + ownerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));
        }

        @Test
        @DisplayName("성공 - MANAGER 전체 결제 조회")
        void successManager() throws Exception {
            // given
            Payment payment = saveDonePayment(order);

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", payment.getId())
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 결제")
        void failNotFound() throws Exception {
            // given
            UUID nonExistentPaymentId = UUID.randomUUID();

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", nonExistentPaymentId)
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("해당하는 결제 내역을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - 타인의 결제를 CUSTOMER가 조회")
        void failOtherCustomer() throws Exception {
            // given
            Payment payment = saveDonePayment(order);

            User otherCustomer = User.builder()
                    .userName("other_" + UUID.randomUUID())
                    .nickName("other_nick_" + UUID.randomUUID())
                    .email("other_" + UUID.randomUUID() + "@example.com")
                    .phoneNumber("010-9999-9999")
                    .role(Role.CUSTOMER)
                    .password("password")
                    .build();
            userRepository.save(otherCustomer);
            String otherToken = jwtSessionService.createJwtSession(otherCustomer.getId()).getAccessToken();

            // when & then
            mockMvc.perform(get("/api/payments/{paymentId}", payment.getId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("결제 목록 조회 (GET /api/payments)")
    class GetPayments {

        @Test
        @DisplayName("성공 - CUSTOMER 본인 결제 목록 페이징 조회")
        void success() throws Exception {
            // given
            saveDonePayment(order);

            // when & then
            mockMvc.perform(get("/api/payments")
                            .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].orderId").value(order.getId().toString()));
        }
    }

    @Nested
    @DisplayName("결제 취소 (POST /api/payments/{paymentId}/cancel)")
    class CancelPayment {

        @Test
        @DisplayName("성공 - CUSTOMER, 취소 가능 시간 이내")
        void success() throws Exception {
            // given
            Payment payment = saveDonePayment(order);
            PaymentCancelRequest request = new PaymentCancelRequest("단순 변심");

            // when & then
            mockMvc.perform(post("/api/payments/{paymentId}/cancel", payment.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.status").value("CANCELED"));

            Payment canceled = paymentRepository.findById(payment.getId()).orElseThrow();
            assertThat(canceled.getStatus()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(canceled.getCancelReason()).isEqualTo("단순 변심");
        }

        @Test
        @DisplayName("실패 - 취소 가능 시간(5분) 초과")
        void failTimeExpired() throws Exception {
            // given (Order.createdAt은 updatable=false라 엔티티 save로는 반영 안 되어 JPQL bulk update로 직접 변경)
            entityManager.createQuery("UPDATE Order o SET o.createdAt = :createdAt WHERE o.id = :id")
                    .setParameter("createdAt", Instant.now().minus(Duration.ofMinutes(10)))
                    .setParameter("id", order.getId())
                    .executeUpdate();
            entityManager.flush();
            entityManager.clear();

            Payment payment = saveDonePayment(order);
            PaymentCancelRequest request = new PaymentCancelRequest("단순 변심");

            // when & then (매니저도 5분 제한 예외 없이 동일 적용)
            mockMvc.perform(post("/api/payments/{paymentId}/cancel", payment.getId())
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("주문 생성 후 5분이 지나 취소할 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - PG 취소 거절 시 DONE 상태로 복구")
        void failPgCancelRejected() throws Exception {
            // given (mockCancel은 reason이 'FAIL'이면 실패하는 임의 규칙)
            Payment payment = saveDonePayment(order);
            PaymentCancelRequest request = new PaymentCancelRequest("FAIL");

            // when & then
            mockMvc.perform(post("/api/payments/{paymentId}/cancel", payment.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502));

            Payment reverted = paymentRepository.findById(payment.getId()).orElseThrow();
            assertThat(reverted.getStatus()).isEqualTo(PaymentStatus.DONE);
        }

        @Test
        @DisplayName("실패 - DONE 상태가 아닌 결제 취소 시도")
        void failInvalidStatus() throws Exception {
            // given (IN_PROGRESS 상태로 저장, approve 호출 안 함)
            Payment payment = Payment.builder()
                    .order(order)
                    .amount(BigDecimal.valueOf(order.getTotalPrice()))
                    .paymentKey("paymentKey_" + UUID.randomUUID())
                    .build();
            paymentRepository.save(payment);

            PaymentCancelRequest request = new PaymentCancelRequest("단순 변심");

            // when & then
            mockMvc.perform(post("/api/payments/{paymentId}/cancel", payment.getId())
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 결제")
        void failNotFound() throws Exception {
            // given
            UUID nonExistentPaymentId = UUID.randomUUID();
            PaymentCancelRequest request = new PaymentCancelRequest("단순 변심");

            // when & then
            mockMvc.perform(post("/api/payments/{paymentId}/cancel", nonExistentPaymentId)
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
