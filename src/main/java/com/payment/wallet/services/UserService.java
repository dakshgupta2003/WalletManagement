package com.payment.wallet.services;

import com.payment.wallet.models.ESUserModel;
import com.payment.wallet.models.UserModel;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public interface UserService {
    ResponseEntity<?> getAllUsers();
    ResponseEntity<?> getSingleUser(Long walletId);
    UserModel addUser(UserModel user);
    ResponseEntity<?> updateUser(Long walletId, Map<String, Object> fieldMap);
    ResponseEntity<?> deleteUser(Long walletId);
    List<ESUserModel> fuzzySearch(String phone, int page, int size);
}
