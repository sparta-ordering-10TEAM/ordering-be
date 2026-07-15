package com.sparta.ordering.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService{

    private final JavaMailSender javaMailSender;

    @Async
    public void sendTempPasswordEmail(String toEmail, String tempPassword) {
        String content = """
        안녕하세요.
        "연봉10조 배달" 서비스입니다.
                
        요청하신 임시 비밀번호는 아래와 같습니다.
        --------------
        %s
        --------------
                
        임시 비밀번호는 발급 시점으로부터 10분 간 유효합니다.
        로그인 후 반드시 비밀번호를 변경해주세요.
                
        감사합니다.        
        """.formatted(tempPassword);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("[연봉10조 배달] 임시 비밀번호 안내");
        message.setText(content);

        javaMailSender.send(message);
    }
}

