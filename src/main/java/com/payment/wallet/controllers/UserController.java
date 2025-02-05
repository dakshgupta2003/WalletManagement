package com.payment.wallet.controllers;

import com.payment.wallet.models.UserModel;
import com.payment.wallet.services.UserService;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers(){
        return this.userService.getAllUsers();
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<?> getSingleUser(@PathVariable Long walletId){
        return this.userService.getSingleUser(walletId);
    }

    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody UserModel userModel) {
        try {
            UserModel savedUser = this.userService.addUser(userModel);
            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating user: " + e.getMessage());
        }
    }

    @PatchMapping("/{walletId}")
    public ResponseEntity<?> updateUser(@PathVariable Long walletId, @RequestBody Map<String, Object> fieldMap){
            return this.userService.updateUser(walletId, fieldMap);
    }

    @DeleteMapping("/{walletId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long walletId){
        return this.userService.deleteUser(walletId);
    }

}
