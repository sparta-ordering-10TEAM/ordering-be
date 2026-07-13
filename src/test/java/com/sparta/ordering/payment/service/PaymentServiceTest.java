package com.sparta.ordering.payment.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.repository.OrderRepository;
import com.sparta.ordering.payment.dto.PGResponse;
import com.sparta.ordering.payment.dto.PaymentRequest;
import com.sparta.ordering.payment.dto.PaymentResponse;
import com.sparta.ordering.payment.entity.Payment;
import com.sparta.ordering.payment.entity.PaymentStatus;
import com.sparta.ordering.payment.repository.PaymentRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.user.entity.Role;
import com.sparta.ordering.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;


    @Nested
    @DisplayName("결제 준비")
    class PreparePayment {
        @Test
        @DisplayName("성공")
        public void test1() {
            // given
            UUID userId = UUID.randomUUID();
            User user = User.builder().build();
            ReflectionTestUtils.setField(user, "id", userId);

            UUID orderId = UUID.randomUUID();
            Order order = Order.create("orderNumber", null, user, "address", "message");
            ReflectionTestUtils.setField(order, "totalPrice", 1000L);
            ReflectionTestUtils.setField(order, "id", orderId);

            PaymentRequest request = new PaymentRequest(orderId, "paymentKey", 1000L);

            when(orderRepository.findByIdAndUser_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(Optional.of(order));
            when(paymentRepository.save(any(Payment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            //when
            Payment payment = paymentService.preparePayment(userId, request);

            //then
            verify(paymentRepository).save(payment);

            assertThat(payment.getPaymentKey()).isEqualTo("paymentKey");
            assertThat(payment.getAmount()).isEqualTo(1000L);
            assertThat(payment.getOrder()).isEqualTo(order);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
            assertThat(payment.getUniqueVersion())
                    .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        }

        @Test
        @DisplayName("실패 - 가격 검증")
        public void test2() {
            // given
            UUID userId = UUID.randomUUID();
            User user = User.builder().build();
            ReflectionTestUtils.setField(user, "id", userId);

            UUID orderId = UUID.randomUUID();
            Order order = Order.create("orderNumber", null, user, "address", "message");
            ReflectionTestUtils.setField(order, "totalPrice", 2000L);
            ReflectionTestUtils.setField(order, "id", orderId);

            PaymentRequest request = new PaymentRequest(orderId, "paymentKey", 1000L);

            when(orderRepository.findByIdAndUser_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(Optional.of(order));

            //when & then
            assertThatThrownBy(() -> paymentService.preparePayment(userId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.PAYMENT_AMOUNT_INVALID);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        public void test3() {
            // given
            UUID userId = UUID.randomUUID();

            UUID orderId = UUID.randomUUID();

            PaymentRequest request = new PaymentRequest(orderId, "paymentKey", 1000L);

            when(orderRepository.findByIdAndUser_IdAndDeletedAtIsNull(orderId, userId))
                    .thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> paymentService.preparePayment(userId, request))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.ORDER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("결제 완료 처리")
    class CompletePayment {
        @Test
        @DisplayName("성공")
        public void test1() {
            // given
            UUID paymentId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .amount(1000L)
                    .paymentKey("paymentKey")
                    .build();
            ReflectionTestUtils.setField(payment, "id", paymentId);

            Instant approvedAt = Instant.now();
            PGResponse response = new PGResponse(approvedAt, "신한카드");

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            // when
            Payment result = paymentService.completePayment(paymentId, response);

            // then
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.DONE);
            assertThat(result.getApprovedAt()).isEqualTo(approvedAt);
            assertThat(result.getCardCompany()).isEqualTo("신한카드");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 결제")
        public void test2() {
            // given
            UUID paymentId = UUID.randomUUID();
            PGResponse response = new PGResponse(Instant.now(), "신한카드");

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.completePayment(paymentId, response))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.PAYMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("결제 실패 처리")
    class FailPayment {
        @Test
        @DisplayName("성공")
        public void test1() {
            // given
            UUID paymentId = UUID.randomUUID();
            Payment payment = Payment.builder()
                    .amount(1000L)
                    .paymentKey("paymentKey")
                    .build();
            ReflectionTestUtils.setField(payment, "id", paymentId);

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            // when
            paymentService.failPayment(paymentId, "카드 승인 거절");

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ABORTED);
            assertThat(payment.getFailReason()).isEqualTo("카드 승인 거절");
            assertThat(payment.getUniqueVersion()).isEqualTo(paymentId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 결제")
        public void test2() {
            // given
            UUID paymentId = UUID.randomUUID();

            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.failPayment(paymentId, "사유"))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.PAYMENT_NOT_FOUND);
        }
    }
    @Nested
    @DisplayName("결제 단건 조회")
    class GetPayment {

        @Test
        @DisplayName("성공 - master/manager")
        void test1() {
            // given
            UUID userId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();
            Order order = createOrder(UUID.randomUUID(), UUID.randomUUID());
            Payment payment = createPayment(paymentId, order);

            when(paymentRepository.findByIdAndDeletedAtIsNull(paymentId))
                    .thenReturn(Optional.of(payment));

            // when
            PaymentResponse response = paymentService.getPayment(paymentId, userId, Role.MANAGER);

            // then
            verify(paymentRepository).findByIdAndDeletedAtIsNull(paymentId);
            verify(paymentRepository, never()).findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(any(), any());
            verify(paymentRepository, never()).findByIdAndOrder_User_IdAndDeletedAtIsNull(any(), any());
            assertThat(response).isEqualTo(PaymentResponse.from(payment));
        }

        @Test
        @DisplayName("성공 - owner")
        void test2() {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();
            Order order = createOrder(UUID.randomUUID(), ownerId);
            Payment payment = createPayment(paymentId, order);

            when(paymentRepository.findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(paymentId, ownerId))
                    .thenReturn(Optional.of(payment));

            // when
            PaymentResponse response = paymentService.getPayment(paymentId, ownerId, Role.OWNER);

            // then
            verify(paymentRepository).findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(paymentId, ownerId);
            assertThat(response).isEqualTo(PaymentResponse.from(payment));
        }

        @Test
        @DisplayName("성공 - customer")
        void test3() {
            // given
            UUID customerId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();
            Order order = createOrder(customerId, UUID.randomUUID());
            Payment payment = createPayment(paymentId, order);

            when(paymentRepository.findByIdAndOrder_User_IdAndDeletedAtIsNull(paymentId, customerId))
                    .thenReturn(Optional.of(payment));

            // when
            PaymentResponse response = paymentService.getPayment(paymentId, customerId, Role.CUSTOMER);

            // then
            verify(paymentRepository).findByIdAndOrder_User_IdAndDeletedAtIsNull(paymentId, customerId);
            assertThat(response).isEqualTo(PaymentResponse.from(payment));
        }

        @Test
        @DisplayName("실패 - owner인데 자기 식당 아님")
        void test4() {
            // given
            UUID ownerId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();

            when(paymentRepository.findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(paymentId, ownerId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.getPayment(paymentId, ownerId, Role.OWNER))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - customer 자기 주문 아님")
        void test5() {
            // given
            UUID customerId = UUID.randomUUID();
            UUID paymentId = UUID.randomUUID();

            when(paymentRepository.findByIdAndOrder_User_IdAndDeletedAtIsNull(paymentId, customerId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> paymentService.getPayment(paymentId, customerId, Role.CUSTOMER))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.PAYMENT_NOT_FOUND);
        }

        private Order createOrder(UUID customerId, UUID restaurantOwnerId) {
            User customer = User.builder().build();
            ReflectionTestUtils.setField(customer, "id", customerId);

            User owner = User.builder().build();
            ReflectionTestUtils.setField(owner, "id", restaurantOwnerId);

            Restaurant restaurant = Restaurant.builder().user(owner).build();

            return Order.create("orderNumber", restaurant, customer, "address", "message");
        }

        private Payment createPayment(UUID paymentId, Order order) {
            Payment payment = Payment.builder()
                    .order(order)
                    .amount(1000L)
                    .paymentKey("paymentKey")
                    .build();
            ReflectionTestUtils.setField(payment, "id", paymentId);
            return payment;
        }
    }
}