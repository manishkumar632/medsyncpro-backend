package com.medsyncpro.utils;

import org.springframework.security.core.Authentication;

import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.exception.ResourceNotFoundException;
import com.medsyncpro.repository.UserRepository;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class Utils {
    private final UserRepository userRepository;

    public User getUserFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("UNAUTHORIZED", "User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "User not found");
        }

        return user;
    }
    
    
}
