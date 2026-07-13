package com.sparta.ordering.payment.client;

import com.sparta.ordering.global.code.ExternalResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.payment.dto.PGResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PaymentClient {

    public PGResponse mockConfirm(Long amount) {
        if (amount >= 1000000) {  // 100만원 이상 실패. 임의로 정한 실패 케이스
            throw new ApiException(ExternalResponseCode.PG_APPROVAL_ERROR);
        }
        return new PGResponse(Instant.now(), "신한카드");
    }
}
