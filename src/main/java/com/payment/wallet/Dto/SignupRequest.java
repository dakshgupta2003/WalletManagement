package com.payment.wallet.Dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SignupRequest {
    private String name;
    private String email;
    private String password;
}
