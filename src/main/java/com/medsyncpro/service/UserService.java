package com.medsyncpro.service;

import com.medsyncpro.dto.RegisterRequest;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "Email already registered");
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());
        
        return userRepository.save(user);
    }
    
    public com.medsyncpro.dto.LoginResponse login(com.medsyncpro.dto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail());
        
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        String token = jwtService.generateToken(user);
        return new com.medsyncpro.dto.LoginResponse(token, user.getEmail(), user.getName(), user.getRole());
    }
}
