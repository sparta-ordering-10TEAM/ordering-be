package com.sparta.ordering.auth.security.customauthentication;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUserNameAndDeletedAtIsNull(username)
                .map(user -> new CustomUserDetails(
                        user.getId(),
                        user.getUserName(),
                        user.getPassword(),
                        user.getRole(),
                        user.isLocked()))
                .orElseThrow(() -> new ApiException(GeneralResponseCode.USER_NOT_FOUND));
    }
}
