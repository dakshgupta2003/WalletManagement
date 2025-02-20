package com.payment.wallet.controllers;

import com.payment.wallet.models.ESUserModel;
import com.payment.wallet.models.UserModel;
import com.payment.wallet.services.UserService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping("/get-all-users")
    public ResponseEntity<?> getAllUsers(){
        return this.userService.getAllUsers();
    }

    @GetMapping("/get-single-user/{walletId}")
    public ResponseEntity<?> getSingleUser(@PathVariable Long walletId){
        return this.userService.getSingleUser(walletId);
    }

    @PostMapping("/create-user")
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

    @PatchMapping("/update-user/{walletId}")
    public ResponseEntity<?> updateUser(@PathVariable Long walletId, @RequestBody Map<String, Object> fieldMap){
            return this.userService.updateUser(walletId, fieldMap);
    }

    @DeleteMapping("/delete-user/{walletId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long walletId){
        return this.userService.deleteUser(walletId);
    }

    @GetMapping("/fuzzy-search")
    public List<ESUserModel> fuzzySearch(
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return this.userService.fuzzySearch(phone, page, size);
    }

}
