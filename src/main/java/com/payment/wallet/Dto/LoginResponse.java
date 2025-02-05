package com.payment.wallet.Dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String refreshToken;
}
