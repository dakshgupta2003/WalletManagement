package com.payment.wallet.models;

import com.payment.wallet.Enum.StatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "transactions")
public class TransactionModel {
    @Id
    private String txnId;
    private Double amount;
    private Long payerWalletId;
    private Long payeeWalletId;
    private StatusEnum status;
    private LocalDateTime timeStamp;
}
