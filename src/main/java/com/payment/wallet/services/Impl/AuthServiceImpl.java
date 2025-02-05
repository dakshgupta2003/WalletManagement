package com.payment.wallet.services.Impl;

import java.util.HashMap;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.payment.wallet.Dto.LoginRequest;
import com.payment.wallet.Dto.LoginResponse;
import com.payment.wallet.Dto.RefreshTokenRequest;
import com.payment.wallet.Dto.SignupRequest;
import com.payment.wallet.Enum.RoleEnum;
import com.payment.wallet.models.Auth;
import com.payment.wallet.repositories.AuthRepository;
import com.payment.wallet.services.AuthService;
import com.payment.wallet.services.JWTService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager; // to verify and validate user email and password
    private final JWTService jwtService;
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    public Auth signUp(SignupRequest request) {
        try {
            // Validate if user already exists
            if (authRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new IllegalArgumentException("User with this email already exists");
            }

            // Validate request data
            validateSignupRequest(request);

            // Create new user
            Auth auth = new Auth();
            auth.setName(request.getName());
            auth.setEmail(request.getEmail().toLowerCase());  // Store email in lowercase
            auth.setPassword(passwordEncoder.encode(request.getPassword()));
            auth.setRole(RoleEnum.USER);

            return authRepository.save(auth);
        } catch (IllegalArgumentException e) {
            throw e;  // Rethrow validation errors
        } catch (Exception e) {
            // Log the error
            logger.error("Error during user registration: {}", e.getMessage());
            throw new RuntimeException("Failed to register user. Please try again later.");
        }
    }

    private void validateSignupRequest(SignupRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getEmail() == null || !request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Valid email is required");
        }
        if (request.getPassword() == null ) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    public LoginResponse login(LoginRequest request) {
        try {
            // Validate request
            validateLoginRequest(request);
            
            logger.debug("Attempting to find user with email: {}", request.getEmail().toLowerCase());
            
            Auth user = authRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found with this email"));
                
            logger.debug("User found, attempting authentication");
            
            try {
                // Attempt authentication
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword());
                
                authenticationManager.authenticate(authToken);
                
                logger.debug("Authentication successful, generating tokens");
                
            } catch (org.springframework.security.core.AuthenticationException e) {
                logger.error("Authentication failed: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid email or password");
            }

            // Generate tokens
            String token = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setRefreshToken(refreshToken);

            logger.info("User {} successfully logged in", user.getEmail());
            return response;

        } catch (IllegalArgumentException e) {
            logger.warn("Login validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during login: ", e);  // Log the full stack trace
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    // get new token from refresh token
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            if (request.getToken() == null || request.getToken().isEmpty()) {
                throw new IllegalArgumentException("Refresh token is required");
            }

            String userEmail = jwtService.extractUsername(request.getToken());
            if (userEmail == null) {
                throw new IllegalArgumentException("Invalid refresh token");
            }

            Auth user = authRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (!jwtService.isTokenValid(request.getToken(), user)) {
                throw new IllegalArgumentException("Refresh token is expired or invalid");
            }

            String newToken = jwtService.generateToken(user);
            
            LoginResponse response = new LoginResponse();
            response.setToken(newToken);
            response.setRefreshToken(request.getToken());
            

            logger.info("Token refreshed successfully for user: {}", user.getEmail());
            return response;

        } catch (IllegalArgumentException e) {
            logger.warn("Token refresh validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error during token refresh: ", e);  // Log full stack trace
            throw new RuntimeException("Failed to refresh token: " + e.getMessage());
        }
    }
}