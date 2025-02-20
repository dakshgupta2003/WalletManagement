package com.payment.wallet.controllers;

import com.payment.wallet.services.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/transaction")
public class TransactionController {

    private final TransactionService transactionService;
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer-fund")
    public ResponseEntity<?> transferFunds(@RequestBody Map<String, Object> request) {
        try {
            String payerPhone = (String) request.get("payerPhone");
            String payeePhone = (String) request.get("payeePhone");
            
            // Handle the amount which might come as Integer or Double
            Object amountObj = request.get("amount");
            double amount;
            if (amountObj instanceof Integer) {
                amount = ((Integer) amountObj).doubleValue();
            } else if (amountObj instanceof Double) {
                amount = (Double) amountObj;
            } else {
                logger.error("Invalid amount type: {}", amountObj != null ? amountObj.getClass() : "null");
                return ResponseEntity.badRequest().body("Invalid amount format");
            }

            // Validate inputs
            if (payerPhone == null || payeePhone == null) {
                return ResponseEntity.badRequest().body("Phone numbers cannot be null");
            }

            return transactionService.transferFunds(payerPhone, payeePhone, amount);
            
        } catch (ClassCastException e) {
            logger.error("Error parsing request parameters: ", e);
            return ResponseEntity.badRequest().body("Invalid request format");
        } catch (Exception e) {
            logger.error("Error processing transaction: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing transaction: " + e.getMessage());
        }
    }

    @GetMapping("/get-all-transactions/{walletId}")
    public ResponseEntity<?> getAllTransactions(@PathVariable Long walletId, 
    @RequestParam(defaultValue = "1") int page, 
    @RequestParam(defaultValue = "10") int size){
        try {
            if(walletId == null){
                return ResponseEntity.badRequest().body("Phone number cannot be null");
            }
            return this.transactionService.getAllTransactions(walletId, page, size);
        } catch (Exception e) {
            logger.error("Error getting all transactions: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error getting all transactions: " + e.getMessage());
        }
        
    }

    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> getTransactionStatus(@PathVariable String transactionId){
        try {
            return this.transactionService.getTransactionStatus(transactionId);
        } catch (Exception e) {
            logger.error("Error getting transaction status: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error getting transaction status: " + e.getMessage());
        }
    }
    
}
