package com.payment.wallet.services;

import com.payment.wallet.Dto.LoginRequest;
import com.payment.wallet.Dto.LoginResponse;
import com.payment.wallet.Dto.RefreshTokenRequest;
import com.payment.wallet.Dto.SignupRequest;
import com.payment.wallet.models.Auth;

public interface AuthService {
    Auth signUp(SignupRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse refreshToken(RefreshTokenRequest request);
}
