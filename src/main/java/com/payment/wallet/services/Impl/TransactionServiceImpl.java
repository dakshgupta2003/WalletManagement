package com.payment.wallet.services.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payment.wallet.Enum.StatusEnum;
import com.payment.wallet.models.TransactionModel;
import com.payment.wallet.models.UserModel;
import com.payment.wallet.repositories.TransactionRepository;
import com.payment.wallet.repositories.UserRepository;

import com.payment.wallet.services.KafkaProducerService;
import com.payment.wallet.services.TransactionService;
import jakarta.transaction.Transactional;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class TransactionServiceImpl implements TransactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionServiceImpl(UserRepository userRepository, TransactionRepository transactionRepository, KafkaProducerService kafkaProducerService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    @Transactional // if anything fails the database changes will be rolled back
    public ResponseEntity<?> transferFunds(String senderPhone, String receiverPhone, double amount) {

        // fetch payer and payee details
        Optional<UserModel> payer = this.userRepository.findByUserPhone(senderPhone);
        Optional<UserModel> payee = this.userRepository.findByUserPhone(receiverPhone);

        if(payer.isEmpty() || payee.isEmpty()){
            logger.info("Payer or Payee not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payer or Payee not found");
        }

        UserModel payerUser = payer.get();
        UserModel payeeUser = payee.get();
        // .get() method is used to retrieve the value present in Optional
        // if it doesn't have any value then it will throw NoSuchElementException

        // check if payer and payee are same
        if(payerUser.getWalletId().equals(payeeUser.getWalletId())){
            logger.info("Payer and Payee cannot be same");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payer and Payee cannot be same");
        }

        if(amount<=0){
            logger.info("Amount is less than 0");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount must be greater than 0");
        }

        // check if payer has enough balance
        if(payerUser.getBalance()<amount){
            logger.info("Payer doesn't have enough balance");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payer doesn't have enough balance");
        }

        payerUser.setBalance(payerUser.getBalance()-amount);
        userRepository.save(payerUser);
        payeeUser.setBalance(payeeUser.getBalance()+amount);
        userRepository.save(payeeUser);

        TransactionModel transaction = new TransactionModel();
        transaction.setTxnId(UUID.randomUUID().toString());
        transaction.setAmount(amount);
        transaction.setPayerWalletId(payerUser.getWalletId());
        transaction.setPayeeWalletId(payeeUser.getWalletId());
        transaction.setStatus(StatusEnum.SUCCESS);
        transaction.setTimeStamp(LocalDateTime.now());

        transactionRepository.save(transaction);

        if(kafkaProducerService !=null){
            try {
                // Convert transaction to JSON
                String transactionObj = objectMapper.writeValueAsString(transaction);
                // Push fund transfer event to Kafka
                kafkaProducerService.fundsPushInKafka(transactionObj);
                logger.info("Published to Kafka: {}", transactionObj);
            } catch (Exception e) {
                logger.error("Failed to publish to Kafka: {}", e.getMessage());
                // Update status before returning
                transaction.setStatus(StatusEnum.PENDING);
                transactionRepository.save(transaction);
                logger.error("Transaction updated to PENDING state");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to publish to Kafka");
            }
        }

        logger.info("Transaction successful");
        return ResponseEntity.status(HttpStatus.OK).body(transaction);
    }

    @Override
    public ResponseEntity<?> getAllTransactions(Long walletId, int page, int size) {
        try {
            Optional<UserModel> user = this.userRepository.findById(walletId);
            if(user.isEmpty()){
                logger.info("User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            
            // check if page is negative
            if(page<0 || size<0){
                logger.info("Page or size cannot be negative");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Page or size cannot be negative");
            }

            if(page>size){
                logger.info("No transactions to display");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No transactions to display");
            }

            // create Pageable object
            Pageable pageable = PageRequest.of(page,size); // e.g. pageNumber = 1, pageSize=10
            // means 10 transactions per page and currently we are on page number 1
            
            UserModel currUser = user.get();
            Page<TransactionModel> pageTransactions = transactionRepository.findByPayerWalletIdOrPayeeWalletId(
                currUser.getWalletId(), 
                currUser.getWalletId(),
                pageable
            );
            logger.info("Total transactions: {}", pageTransactions.getTotalElements());
            logger.info("Pagenation Info: {}", pageTransactions.getPageable());
            logger.info("Total pages: {}", pageTransactions.getTotalPages());

            Map<String, Object> response = new LinkedHashMap<>(); // ordered maps
            response.put("Total Transactions", pageTransactions.getTotalElements());
            response.put("PaginationInfo", "pageNumber:" + pageTransactions.getPageable().getPageNumber() + " " + "pageSize:" + pageTransactions.getPageable().getPageSize());
            response.put("TotalPages", pageTransactions.getTotalPages());
            response.put("Transactions", pageTransactions.getContent());
            
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            logger.error("Error fetching transactions : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching transactions");
        }
    }

    public ResponseEntity<?> getTransactionStatus(String transactionId){
        try {
            Optional<TransactionModel> transaction = transactionRepository.findById(transactionId);
            if(transaction.isEmpty()){
                logger.info("Transaction not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found");
            }
            return ResponseEntity.status(HttpStatus.OK).body(transaction.get().getStatus());
        } catch (Exception e) {
            logger.error("Error fetching transaction status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching transaction status");
        }
    }

}
