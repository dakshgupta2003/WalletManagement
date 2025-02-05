package com.payment.wallet.repositories;

import com.payment.wallet.models.TransactionModel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionModel, String> {
    Page<TransactionModel> findByPayerWalletIdOrPayeeWalletId(
        Long payerWalletId,
        Long payeeWalletId,
        Pageable pageable
    );
}
