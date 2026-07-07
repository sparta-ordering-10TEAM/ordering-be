package com.sparta.ordering.user.service;

import com.sparta.ordering.user.entity.User;
import com.sparta.ordering.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("잠금 상태 변경 성공")
    void updateLock() {
        // given
        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .locked(false)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));

        // when
        adminService.lock(userId);

        // then
        assertThat(user.isLocked()).isEqualTo(true);
    }

}