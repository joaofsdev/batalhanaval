package com.softexpert.batalhanaval_api.service;

import com.softexpert.batalhanaval_api.domain.User;
import com.softexpert.batalhanaval_api.dto.request.LoginRequest;
import com.softexpert.batalhanaval_api.dto.request.RegisterRequest;
import com.softexpert.batalhanaval_api.dto.response.AuthResponse;
import com.softexpert.batalhanaval_api.exception.AccountDisabledException;
import com.softexpert.batalhanaval_api.exception.EmailAlreadyTakenException;
import com.softexpert.batalhanaval_api.exception.InvalidCredentialsException;
import com.softexpert.batalhanaval_api.exception.UsernameAlreadyTakenException;
import com.softexpert.batalhanaval_api.repository.UserRepository;
import com.softexpert.batalhanaval_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyTakenException();
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyTakenException();
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), token);
    }
}
