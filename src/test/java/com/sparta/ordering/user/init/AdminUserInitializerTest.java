package com.sparta.ordering.user.init;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder= new BCryptPasswordEncoder();;

    @InjectMocks
    private AdminUserInitializer adminUserInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminUserInitializer, "masterEmail", "master_test@mail.com");
        ReflectionTestUtils.setField(adminUserInitializer, "masterName", "master");
        ReflectionTestUtils.setField(adminUserInitializer, "masterPassword", "masterTest!");
        ReflectionTestUtils.setField(adminUserInitializer, "masterNickName", "testMaster");
        ReflectionTestUtils.setField(adminUserInitializer, "masterPhoneNumber", "010-0000-0000");
    }

    @Test
    void 관리자_생성_성공() throws Exception {
        // given
        when(userRepository.existsByEmailAndDeletedAtIsNull("master_test@mail.com")).thenReturn(false);
        when(userRepository.existsByUserNameAndDeletedAtIsNull("master")).thenReturn(false);
        when(userRepository.existsByNickNameAndDeletedAtIsNull("testMaster")).thenReturn(false);

        // save()가 호출될 때 어떤 User 객체가 저장되었는지 확인하기 위해 ArgumentCaptor로 캡처
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(any(User.class))).thenAnswer(
                invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        adminUserInitializer.run(null);

        // then
        verify(userRepository).save(captor.capture());  // 호출된 user 객체를 captor에 저장
        User savedUser = captor.getValue(); // 캡처된 인자 꺼내서 검증
        assertThat(savedUser.getUserName()).isEqualTo("master");
        assertThat(savedUser.getEmail()).isEqualTo("master_test@mail.com");
    }

}