package com.payment.wallet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.wallet.Enum.StatusEnum;
import com.payment.wallet.models.TransactionModel;
import com.payment.wallet.models.UserModel;
import com.payment.wallet.repositories.TransactionRepository;
import com.payment.wallet.repositories.UserRepository;
import com.payment.wallet.services.Impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@SpringBootTest
public class TransactionServiceImplTests {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private TransactionServiceImpl transactionService;


    private UserModel payer, payee;
    private TransactionModel transaction1,transaction2;

    @BeforeEach
    void setUp(){
        payer = new UserModel();
        payer.setWalletId(1L);
        payer.setUserPhone("1234567890");
        payer.setBalance(100.0);

        payee = new UserModel();
        payee.setWalletId(2L);
        payee.setUserPhone("1234567891");
        payee.setBalance(200.0);

        transaction1 = new TransactionModel();
        transaction1.setTxnId("txn123");
        transaction1.setPayerWalletId(1L);
        transaction1.setPayeeWalletId(2L);
        transaction1.setAmount(100.0);
        transaction1.setStatus(StatusEnum.SUCCESS);

        transaction2 = new TransactionModel();
        transaction2.setTxnId("txn456");
        transaction2.setPayerWalletId(2L);
        transaction2.setPayeeWalletId(1L);
        transaction2.setAmount(200.0);
        transaction2.setStatus(StatusEnum.SUCCESS);

    }

    @Test
    void shouldTransferFundsAndPublishInKafkaWhenSenderAndReceiverExistsAndSenderHasSufficientBalance(){
        when(userRepository.findByUserPhone("1234567890")).thenReturn(Optional.of(payer));
        when(userRepository.findByUserPhone("1234567891")).thenReturn(Optional.of(payee));
        when(userRepository.save(payer)).thenReturn(payer);
        when(userRepository.save(payee)).thenReturn(payee);
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.setPayerWalletId(payer.getWalletId());
        transactionModel.setPayeeWalletId(payee.getWalletId());
        transactionModel.setAmount(20.0);
        transactionModel.setStatus(StatusEnum.SUCCESS);
        when(transactionRepository.save(any(TransactionModel.class))).thenReturn(transactionModel);
        ResponseEntity<?> response = transactionService.transferFunds("1234567890", "1234567891", 20.0);

        assertEquals(OK, response.getStatusCode());
        verify(transactionRepository, times(1)).save(any(TransactionModel.class));
        verify(userRepository, times(2)).save(any(UserModel.class));
        verify(kafkaProducerService, times(1)).fundsPushInKafka(anyString());

    }

    @Test
    void shouldReturnNotFoundIfSenderORReceiverDoesNotExist(){
        when(userRepository.findByUserPhone("1234567890")).thenReturn(Optional.empty());

        ResponseEntity<?> response = transactionService.transferFunds("1234567890", "0987654321", 200.0);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Payer or Payee not found", response.getBody());
    }

    @Test
    void shouldReturnBadRequestIfSenderAndReceiverAreSame() {
        when(userRepository.findByUserPhone("1234567890")).thenReturn(Optional.of(payer));
        // no need to write for payee as we are sending same phoneNumbers so when userRepo is called for payee then also the mocked result i.e. payer will be returned
        ResponseEntity<?> response = transactionService.transferFunds("1234567890", "1234567890", 200.0);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Payer and Payee cannot be same", response.getBody());
        verify(userRepository, times(2)).findByUserPhone("1234567890");
        verify(userRepository,never()).save(any(UserModel.class));
        verify(transactionRepository, never()).save(any(TransactionModel.class));
    }

    @Test
    void shouldReturnBadRequestIfAmountIsZeroOrNegative() {

        when(userRepository.findByUserPhone("1234567890")).thenReturn(Optional.of(payer));
        when(userRepository.findByUserPhone("0987654321")).thenReturn(Optional.of(payee));

        ResponseEntity<?> responseZero = transactionService.transferFunds("1234567890", "0987654321", 0);
        ResponseEntity<?> responseNegative = transactionService.transferFunds("1234567890", "0987654321", -100);

        assertEquals(HttpStatus.BAD_REQUEST, responseZero.getStatusCode());
        assertEquals("Amount must be greater than 0", responseZero.getBody());

        assertEquals(HttpStatus.BAD_REQUEST, responseNegative.getStatusCode());
        assertEquals("Amount must be greater than 0", responseNegative.getBody());
    }

    @Test
    void shouldReturnBadRequestIfSenderDoesNotHaveSufficientBalance() {
        payer.setBalance(100.0); // Less than transfer amount
        when(userRepository.findByUserPhone("1234567890")).thenReturn(Optional.of(payer));
        when(userRepository.findByUserPhone("0987654321")).thenReturn(Optional.of(payee));

        ResponseEntity<?> response = transactionService.transferFunds("1234567890", "0987654321", 200.0);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Payer doesn't have enough balance", response.getBody());
    }

    @Test
    void shouldReturnInternalServerErrorAndSetTransactionStatusPendingIfKafkaPublishingFails(){
        when(userRepository.findByUserPhone("1234567890")).thenReturn(Optional.of(payer));
        when(userRepository.findByUserPhone("1234567891")).thenReturn(Optional.of(payee));
        when(userRepository.save(payer)).thenReturn(payer);
        when(userRepository.save(payee)).thenReturn(payee);

        doThrow(new RuntimeException("Kafka publish failed")).when(kafkaProducerService).fundsPushInKafka(anyString());

        ResponseEntity<?> response = transactionService.transferFunds("1234567890", "1234567891", 20.0);

        assertEquals(INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to publish to Kafka", response.getBody());
        verify(transactionRepository, times(2)).save(argThat(transaction -> transaction.getStatus().equals(StatusEnum.PENDING)));

    }

    // getAllTransactions

    @Test
    void shouldReturnAllTransactionsIfTransactionExists(){
        when(userRepository.findById(1L)).thenReturn(Optional.of(payer));

        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionModel> pageTransaction = new PageImpl<>(List.of(transaction1,transaction2),pageable,2);
        when(transactionRepository.findByPayerWalletIdOrPayeeWalletId(eq(1L),eq(1L), any(PageRequest.class))).thenReturn(pageTransaction);
        // find this walletId in payer or payee

        ResponseEntity<?> response = transactionService.getAllTransactions(1L, 0, 10);

        assertEquals(OK, response.getStatusCode());
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals(2L, responseBody.get("Total Transactions"));
        assertTrue(responseBody.get("Transactions").toString().contains("txn123"));
        assertTrue(responseBody.get("Transactions").toString().contains("txn456"));
    }

    @Test
    void shouldReturnEmptyTransactionListIfNoTransactionsExistForWalletId() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionModel> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(transactionRepository.findByPayerWalletIdOrPayeeWalletId(eq(1L), eq(1L), any(PageRequest.class)))
                .thenReturn(emptyPage);

        ResponseEntity<?> response = transactionService.getAllTransactions(1L, 0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Total Transactions=0"));
    }

    @Test
    void shouldReturnBadRequestIfPageNumberIsGreaterThanTotalPages() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(payer));
        Page<TransactionModel> emptyPage = new PageImpl<>(List.of());
        when(transactionRepository.findByPayerWalletIdOrPayeeWalletId(eq(1L), eq(1L), any(PageRequest.class)))
                .thenReturn(emptyPage);

        ResponseEntity<?> response = transactionService.getAllTransactions(1L, 11, 10); // More than available pages

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No transactions to display", response.getBody());
    }

    @Test
    void shouldReturnInternalServerErrorIfExceptionOccursWhileFetchingTransactions() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("Database Error"));

        ResponseEntity<?> response = transactionService.getAllTransactions(1L, 0, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error fetching transactions", response.getBody());
    }

    @Test
    void shouldReturnTransactionStatusIfTransactionExists(){
        when(transactionRepository.findById("txn123")).thenReturn(Optional.of(transaction1));

        ResponseEntity<?> response = transactionService.getTransactionStatus("txn123");

        assertEquals(OK, response.getStatusCode());
        assertEquals(StatusEnum.SUCCESS, response.getBody());
    }

    @Test
    void shouldReturnBadRequestIfTransactionDoesNotExist() {
        when(transactionRepository.findById("txn123")).thenReturn(Optional.empty());

        ResponseEntity<?> response = transactionService.getTransactionStatus("txn123");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Transaction not found", response.getBody());
    }

    @Test
    void shouldReturnInternalServerErrorIfExceptionOccursWhileFetchingTransactionStatus() {
        when(transactionRepository.findById("txn123")).thenThrow(new RuntimeException("Database Error"));

        ResponseEntity<?> response = transactionService.getTransactionStatus("txn123");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error fetching transaction status", response.getBody());
    }




}
