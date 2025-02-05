package com.payment.wallet.services;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public interface TransactionService {
    ResponseEntity<?> transferFunds(String senderPhone, String receiverPhone, double amount);
    ResponseEntity<?> getAllTransactions(Long walletId, int page, int size);
    ResponseEntity<?> getTransactionStatus(String transactionId);
}
