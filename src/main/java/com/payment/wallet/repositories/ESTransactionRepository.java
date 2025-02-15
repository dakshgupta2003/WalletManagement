package com.payment.wallet.repositories;

import com.payment.wallet.models.ESTransactionModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ESTransactionRepository extends ElasticsearchRepository<ESTransactionModel, String> {

}
