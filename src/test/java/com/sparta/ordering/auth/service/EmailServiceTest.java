package com.sparta.ordering.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void 비밀번호_초기화_이메일을_전송한다() {
        // given
        String toEmail = "user@example.com";
        String tempPassword = "TempPass123!";

        // when
        emailService.sendTempPasswordEmail(toEmail, tempPassword);

        // then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly(toEmail);
        assertThat(sent.getSubject()).isEqualTo("[연봉10조 배달] 임시 비밀번호 안내");
        assertThat(sent.getText()).contains(tempPassword);
    }
}