package com.payment.wallet.repositories;

import com.payment.wallet.models.ESUserModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ESUserRepository extends ElasticsearchRepository<ESUserModel, Long> {

}
