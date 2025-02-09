package com.payment.wallet.services.Impl;

import com.payment.wallet.models.UserModel;
import com.payment.wallet.repositories.UserRepository;
import com.payment.wallet.services.KafkaProducerService;
import com.payment.wallet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.util.ReflectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final KafkaProducerService kafkaProducerService;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final ObjectMapper objectMapper;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, KafkaProducerService kafkaProducerService) {
        this.userRepository = userRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = new ObjectMapper();
    }

    

    public ResponseEntity<?> getAllUsers(){
        try {
            List<UserModel> users = userRepository.findAll();
            if(users==null || users.isEmpty()){
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.status(HttpStatus.OK).body(users);
        } catch(DataAccessException e){
            logger.error("Databse access error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database access error");
        } 
        catch (Exception e) {
            logger.error("Unexpected error while fetching users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error while fetching users");
        }
    }

    public ResponseEntity<?> getSingleUser(Long walletId){
        try {
            UserModel user = userRepository.findById(walletId).orElse(null);
            if(user == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            return ResponseEntity.status(HttpStatus.OK).body(user);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error while fetching user");
        }
    }

    @Override
    public UserModel addUser(UserModel user) {
        try {
            // Validate input
            if (user.getUserPhone() == null || user.getUserPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number cannot be null or empty");
            }

            // Check for existing user
            Optional<UserModel> existingUser = userRepository.findByUserPhone(user.getUserPhone());
            if (existingUser.isPresent()) {
                throw new IllegalArgumentException("User with this phone number already exists");
            }

            // Save the new user
            userRepository.save(user);

            if(kafkaProducerService !=null){
                try {
                    // Convert user to JSON
                    String userObj = objectMapper.writeValueAsString(user);
                    // Push user creation event to Kafka
                    kafkaProducerService.userPushInKafka(userObj);
                    logger.info("Published to Kafka: {}", userObj);
                } catch (Exception e) {
                    logger.error("Failed to publish to Kafka: {}", e.getMessage());
                    logger.error("User creation event not published to Kafka");
                }
            }

            logger.info("user added succesfully with ID: {}", user.getWalletId());
            return user;

        } catch (Exception e) {
            logger.error("Unexpected error while adding user: {}", e.getMessage());
            throw e;
        }
    }

    public ResponseEntity<?> updateUser(Long walletId, Map<String, Object> fieldMap) {
        try {
            UserModel user = userRepository.findById(walletId).orElse(null);
            if(user == null){
                logger.info("User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            for(String key : fieldMap.keySet()){
                Object value = fieldMap.get(key);
                Field field = ReflectionUtils.findField(UserModel.class, key);
                if (field == null) {
                    logger.warn("Field {} not found in UserModel", key);
                    continue;
                }
                field.setAccessible(true);
                try {
                    // Handle numeric type conversion for balance
                    if ("balance".equals(key) && value instanceof Number) {
                        ReflectionUtils.setField(field, user, ((Number) value).doubleValue());
                    } else {
                        ReflectionUtils.setField(field, user, value);
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("Error setting field {}: {}", key, e.getMessage());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid value for field: " + key);
                }
            }

            UserModel updatedUser = userRepository.save(user);
            logger.info("User updated successfully with ID: {}", updatedUser.getWalletId());
            return ResponseEntity.status(HttpStatus.OK).body(updatedUser);
            
        } catch (Exception e) {
            logger.error("Unexpected error while updating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error while updating user");
        }
    }

    public ResponseEntity<?> deleteUser(Long walletId){
        try {
            UserModel user = userRepository.findById(walletId).orElse(null);
            if(user == null){
                logger.info("User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            userRepository.deleteById(walletId);
            return ResponseEntity.status(HttpStatus.OK).body("User deleted successfully");
        } catch (Exception e) {
            logger.error("Unexpected error while deleting user: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    
}
