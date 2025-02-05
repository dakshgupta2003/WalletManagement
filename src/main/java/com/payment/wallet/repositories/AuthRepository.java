package com.payment.wallet.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.payment.wallet.Enum.RoleEnum;
import com.payment.wallet.models.Auth;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByEmail(String email);
    Auth findByRole(RoleEnum role);
}
