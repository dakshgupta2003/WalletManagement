package com.payment.wallet.models;

import com.payment.wallet.Enum.StatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timeStamp;
}
