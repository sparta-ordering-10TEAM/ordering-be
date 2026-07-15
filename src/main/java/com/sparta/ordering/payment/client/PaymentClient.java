package com.sparta.ordering.payment.client;

import com.sparta.ordering.global.code.ExternalResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.payment.dto.PGCancelRequest;
import com.sparta.ordering.payment.dto.PGCancelResponse;
import com.sparta.ordering.payment.dto.PGResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class PaymentClient {

    public PGResponse mockConfirm(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.valueOf(1000000)) >= 0) {  // 100만원 이상 실패. 임의로 정한 실패 케이스
            throw new ApiException(ExternalResponseCode.PG_APPROVAL_ERROR);
        }
        return new PGResponse(Instant.now(), "신한카드");
    }

    public PGCancelResponse mockCancel(PGCancelRequest request) {
        if ("FAIL".equals(request.reason())) {  // 임의로 정한 실패 케이스
            throw new ApiException(ExternalResponseCode.PG_CANCEL_ERROR);
        }
        return new PGCancelResponse(Instant.now(), request.reason());
    }
}
