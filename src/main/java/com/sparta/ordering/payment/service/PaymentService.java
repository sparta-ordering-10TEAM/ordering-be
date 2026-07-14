package com.sparta.ordering.payment.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.order.entity.Order;
import com.sparta.ordering.order.repository.OrderRepository;
import com.sparta.ordering.payment.dto.PGCancelResponse;
import com.sparta.ordering.payment.dto.PGResponse;
import com.sparta.ordering.payment.dto.PaymentRequest;
import com.sparta.ordering.payment.dto.PaymentResponse;
import com.sparta.ordering.payment.entity.Payment;
import com.sparta.ordering.payment.entity.PaymentStatus;
import com.sparta.ordering.payment.repository.PaymentRepository;
import com.sparta.ordering.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    private static final Duration CANCEL_TIME_LIMIT = Duration.ofMinutes(5);

    @Transactional
    public Payment preparePayment(UUID userId, PaymentRequest request) {
        // 1 . 주문 조회 & 소유자 검증
        Order order = orderRepository.findByIdAndUser_IdAndDeletedAtIsNull(request.orderId(), userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.ORDER_NOT_FOUND));

        // 가격 검증
        if (!order.getTotalPrice().equals(request.amount())) {
            throw new ApiException(GeneralResponseCode.PAYMENT_AMOUNT_INVALID);
        }

        // Payment(IN_PROGRESS) 저장 - 동시 중복 요청은 유니크 제약에서 걸러짐
        Payment payment = Payment.builder()
                .order(order)
                .amount(request.amount())
                .paymentKey(request.paymentKey())
                .build();

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment completePayment(UUID paymentId, PGResponse response) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PAYMENT_NOT_FOUND));

        if (!payment.getStatus().equals(PaymentStatus.IN_PROGRESS)) {
            throw new ApiException(GeneralResponseCode.PAYMENT_INVALID_PAYMENT_STATUS);
        }

        payment.approve(response.approvedAt(), response.cardCompany());
        return payment;
    }

    @Transactional
    public void failPayment(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PAYMENT_NOT_FOUND));

        if (!payment.getStatus().equals(PaymentStatus.IN_PROGRESS)) {
            throw new ApiException(GeneralResponseCode.PAYMENT_INVALID_PAYMENT_STATUS);
        }

        payment.fail(reason);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId, UUID userId, Role role) {
        Payment payment = findAccessiblePayment(paymentId, userId, role);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(UUID userId, Role role, Pageable pageable) {
        Page<Payment> payments = switch (role) {
            case MANAGER, MASTER -> paymentRepository.findAllByDeletedAtIsNull(pageable); // master, manager 조회
            case OWNER -> paymentRepository.findAllByOrder_Restaurant_User_IdAndDeletedAtIsNull(userId, pageable); // owner는 자기 가게 주문인지 확인
            case CUSTOMER -> paymentRepository.findAllByOrder_User_IdAndDeletedAtIsNull(userId, pageable); // customer는 자기 주문인지 확인
        };
        return payments.map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public Payment prepareCancelPayment(UUID paymentId, UUID userId, Role role) {
        Payment payment = findAccessiblePayment(paymentId, userId, role);

        if (!payment.getStatus().equals(PaymentStatus.DONE)) {
            throw new ApiException(GeneralResponseCode.PAYMENT_INVALID_PAYMENT_STATUS);
        }

        // 관리자(MANAGER/MASTER)는 5분 제한 없이 취소 가능
        if (role != Role.MANAGER && role != Role.MASTER) {
            Instant cancelDeadline = payment.getOrder().getCreatedAt().plus(CANCEL_TIME_LIMIT);
            if (Instant.now().isAfter(cancelDeadline)) {
                throw new ApiException(GeneralResponseCode.PAYMENT_CANCEL_TIME_EXPIRED);
            }
        }

        return payment;
    }

    @Transactional
    public Payment cancelPayment(UUID paymentId, PGCancelResponse response) {
        Payment payment = paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PAYMENT_NOT_FOUND));

        payment.cancel(response.reason(), response.canceledAt());
        return payment;
    }

    private Payment findAccessiblePayment(UUID paymentId, UUID userId, Role role) {
        return switch (role) {
            case MANAGER, MASTER -> paymentRepository.findByIdAndDeletedAtIsNull(paymentId) // master, manager 조회 가능
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.PAYMENT_NOT_FOUND));
            case OWNER -> paymentRepository.findByIdAndOrder_Restaurant_User_IdAndDeletedAtIsNull(paymentId, userId) // owner는 자기 가게 주문인지 확인
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.PAYMENT_NOT_FOUND));
            case CUSTOMER -> paymentRepository.findByIdAndOrder_User_IdAndDeletedAtIsNull(paymentId, userId) // customer는 자기 주문인지 확인
                    .orElseThrow(() -> new ApiException(GeneralResponseCode.PAYMENT_NOT_FOUND));
        };
    }
}
