package com.payment.wallet.repositories;

import com.payment.wallet.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface UserRepository extends JpaRepository<UserModel, Long> {
    // optional is to handle the case if user phone doesn't exist
    Optional<UserModel> findByUserPhone(String userPhone);
}
