package com.payment.wallet.services;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.wallet.models.UserModel;
import com.payment.wallet.repositories.UserRepository;
import com.payment.wallet.services.Impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@SpringBootTest
public class UserServiceImplTests {

    @Mock // creates a dummy/fake repository for testing
    private UserRepository userRepository;

    @InjectMocks // injects that mock repo in the class we need to test
    private UserServiceImpl userService;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    private UserModel user1, user2;


    // set up before each test case
    @BeforeEach
    void setUp(){
        user1 = UserModel.builder().walletId(1L).balance(1000.0).userPhone("1234567890").build();
        user2 = UserModel.builder().walletId(2L).balance(2000.0).userPhone("1234567891").build();
    }


    // getAllUsers()

    @Test
    void shouldReturnAllUsersWhenUsersExist_getAllUsers() {
        List<UserModel> users = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(users); // when findAll() is called, return the list of users we created for testing

        // call service method
        ResponseEntity<?> response = userService.getAllUsers();

        // verify response
        assertEquals(OK,response.getStatusCode());
        assertEquals(users, response.getBody());

        //verify repository interaction
        verify(userRepository, times(1)).findAll();
        // check if findAll() was called 1 time
    }

    @Test
    void shouldReturnNotFoundWhenNoUserExists_getAllUsers(){
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        ResponseEntity<?> response = userService.getAllUsers();
        assertEquals(NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnServiceUnavailableForDatabaseException_getAllUsers(){
        when(userRepository.findAll()).thenThrow(new DataAccessException("Database Error") {
        });

        ResponseEntity<?> response = userService.getAllUsers();

        assertEquals(SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Database access error", response.getBody());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedError_getAllUsers(){
        when(userRepository.findAll()).thenThrow(new RuntimeException("Unexpected Error"){});

        ResponseEntity<?> response = userService.getAllUsers();
        assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error while fetching users", response.getBody());

        verify(userRepository, times(1)).findAll();
    }


    // getSingleUser()

    @Test
    void shouldReturnUserDetailsWhenUserExists_getSingleUser(){
        Long walletId = 1L;
        when(userRepository.findById(walletId)).thenReturn(Optional.ofNullable(user1));

        ResponseEntity<?> response = userService.getSingleUser(1L);

        assertEquals(OK, response.getStatusCode());
        assertEquals(user1, response.getBody());

        verify(userRepository, times(1)).findById(walletId);
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist_getSingleUser(){
        Long walletId = 3L;
        when(userRepository.findById(walletId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = userService.getSingleUser(walletId);

        assertEquals(NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody());

        verify(userRepository, times(1)).findById(walletId);
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedError_getSingleUser(){
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Unexpected Error"){});

        ResponseEntity<?> response = userService.getSingleUser(1L);

        assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error while fetching user", response.getBody());

        verify(userRepository, times(1)).findById(1L);
    }

    // addUser()

    @Test
    void shouldAddUserWhenValidDataISProvided_addUser() throws JsonProcessingException {
        String PhoneNumber = "1234567890";
        when(userRepository.findByUserPhone(PhoneNumber)).thenReturn(Optional.empty());
        // return empty to ensure that user doesn't exist already and we'll create a new user
        String userObj = "{\"walletId\":1,\"balance\":1000.0,\"userPhone\":\"1234567890\"}";
        when(objectMapper.writeValueAsString(user1)).thenReturn(userObj);
        when(userRepository.save(user1)).thenReturn(user1);


        UserModel response = userService.addUser(user1);
        // this adds the user i.e. --> checks if user already exists, saves the user, converts the user object into string, publishes in kafka

        assertNotNull(response);
        assertEquals(user1,response);

        verify(userRepository, times(1)).findByUserPhone(PhoneNumber);
        verify(userRepository, times(1)).save(user1);
        verify(kafkaProducerService, times(1)).userPushInKafka(userObj);
    }


    @Test
    void shouldThrowIllegalArgumentExceptionWhenPhoneIsNullOrEmpty_addUser(){
        UserModel invalidUser = UserModel.builder().walletId(1L).balance(1000.0).userPhone("").build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> userService.addUser(invalidUser));
        // when addUser() is called with invalidUser, it should throw an IllegalArgumentException

        assertEquals("Phone number cannot be null or empty", exception.getMessage());

        verify(userRepository, never()).save(any());
        // never() ensures that method inside userRepository i.e. save() was never invoked
        // this is done because we are expecting an exception to be thrown
        // save(any()) checks that save was never called with any argument

    }

    @Test
    void shouldReturnIllegalArgumentExceptionWhenUserAlreadyExists_addUser() {
        when(userRepository.findByUserPhone(user1.getUserPhone())).thenReturn(Optional.of(user1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()-> userService.addUser(user1));

        assertEquals("User with this phone number already exists", exception.getMessage());

        verify(userRepository, times(1)).findByUserPhone(user1.getUserPhone());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldHandleKafkaPublishingFailure_addUser() throws JsonProcessingException {
        // to simulate the kafka publish error behaviour
        when(userRepository.findByUserPhone(user1.getUserPhone())).thenReturn(Optional.empty());
        when(userRepository.save(user1)).thenReturn(user1);
        String userObj = "{\"walletId\":1,\"balance\":1000.0,\"userPhone\":\"1234567890\"}";
        when(objectMapper.writeValueAsString(user1)).thenReturn(userObj);
//        when(kafkaProducerService.userPushInKafka(userObj)).thenThrow(new RuntimeException("Kafka Error")); --> wrong, userPushInKafka doesn't return anythign i.e. void so we can't write thenThrow or thenReturn
        doThrow(new RuntimeException("Kafka Error")).when(kafkaProducerService).userPushInKafka(userObj);

        assertDoesNotThrow(() -> userService.addUser(user1)); // it ensures that addUser() doesn't throw any exception
        // because we need to check only kafka error in this test case

        verify(userRepository, times(1)).findByUserPhone(user1.getUserPhone());
        verify(userRepository, times(1)).save(user1);
        verify(kafkaProducerService, times(1)).userPushInKafka(userObj);

    }

    @Test
    void shouldThrowErrorForUnexpectedException_addUser(){
        when(userRepository.findByUserPhone(user1.getUserPhone())).thenThrow(new RuntimeException("Unexpected Error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.addUser(user1));

        assertEquals("Unexpected Error", exception.getMessage());

        verify(userRepository, never()).save(any());
        verify(kafkaProducerService, never()).userPushInKafka(any());
    }


    // updateUser()

    @Test
    void shouldUpdateExistingUser_updateUser(){
        when(userRepository.findById(user1.getWalletId())).thenReturn(Optional.of(user1));
        Map<String, Object> fieldMap = Map.of("balance", 5000.0);
        when(userRepository.save(user1)).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = userService.updateUser(user1.getWalletId(), fieldMap);

        assertEquals(OK, response.getStatusCode());
        UserModel updatedUser = (UserModel) response.getBody();
        assertNotNull(updatedUser);
        assertEquals(5000.0, updatedUser.getBalance());

        verify(userRepository, times(1)).findById(user1.getWalletId());
        verify(userRepository, times(1)).save(user1);

    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist_updateUser(){
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        Map<String, Object> fieldMap = Map.of("balance", 5000.0);

        ResponseEntity<?> response = userService.updateUser(1L, fieldMap);

        assertEquals(NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody());

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedError_updateUser(){
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Unexpected Error"));

        Map<String, Object> fieldMap = Map.of("balance", 5000.0);

        ResponseEntity<?> response = userService.updateUser(1L, fieldMap);

        assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error while updating user", response.getBody());

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any());
    }


    // deleteUser()

    @Test
    void shouldDeleteAnExistingUser_deleteUser(){
        when(userRepository.findById(user1.getWalletId())).thenReturn(Optional.of(user1));
        doNothing().when(userRepository).deleteById(user1.getWalletId());

        ResponseEntity<?> response = userService.deleteUser(user1.getWalletId());

        assertEquals(OK, response.getStatusCode());
        assertEquals("User deleted successfully", response.getBody());
        
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist_deleteUser(){
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = userService.deleteUser(1L);

        assertEquals(NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody());

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).deleteById(1L);
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedError_deleteUser(){
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Unexpected Error"));

        ResponseEntity<?> response = userService.deleteUser(1L);

        assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error while deleting user", response.getBody());

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).deleteById(1L);
    }

    // fuzzy search

    @Test
    void shouldReturnSearchResultWhenMatchingUserExists_fuzzySearch(){

    }


}
