package com.sparta.ordering.payment.facade;

import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.payment.client.PaymentClient;
import com.sparta.ordering.payment.dto.PGResponse;
import com.sparta.ordering.payment.dto.PaymentRequest;
import com.sparta.ordering.payment.dto.PaymentResponse;
import com.sparta.ordering.payment.entity.Payment;
import com.sparta.ordering.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final PaymentClient paymentClient;

    public PaymentResponse processPayment(UUID userId, PaymentRequest request) {

        // 1. 결제 준비 (트랜잭션) - 주문 검증 + Payment 생성
        Payment payment = paymentService.preparePayment(userId, request);

        try {
            // 2. 외부 PG 승인 요청 (트랜잭션 밖)
            PGResponse pgResponse = paymentClient.mockConfirm(request.amount());

            // 3. 결과 반영 - 성공 (트랜잭션)
            Payment completed = paymentService.completePayment(payment.getId(), pgResponse);
            return PaymentResponse.from(completed);
        } catch (ApiException e) {
            // 3. 결과 반영 - 실패 (트랜잭션)
            paymentService.failPayment(payment.getId(), e.getMessage());
            throw e;
        }
        // 실제 PG 연동을 한다면, 네트워크 오류/타임아웃은 별도로 예외 처리 (이중 결제 위험)
    }
}
