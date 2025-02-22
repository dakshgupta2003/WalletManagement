package com.payment.wallet.mappers;

import com.payment.wallet.models.TransactionModel;
import com.payment.wallet.models.ESTransactionModel;
import com.payment.wallet.utils.DateTimeUtils;

public class TransactionMapper {
    public static ESTransactionModel toESModel(TransactionModel transaction) {
        ESTransactionModel esModel = new ESTransactionModel();
        esModel.setTxnId(transaction.getTxnId());
        esModel.setAmount(transaction.getAmount());
        esModel.setPayerWalletId(transaction.getPayerWalletId());
        esModel.setPayeeWalletId(transaction.getPayeeWalletId());
        esModel.setStatus(transaction.getStatus());
        esModel.setTimeStamp(DateTimeUtils.formatDateTime(transaction.getTimeStamp()));
        return esModel;
    }
} 