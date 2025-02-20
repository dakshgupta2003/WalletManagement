package com.payment.wallet.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.wallet.models.ESTransactionModel;
import com.payment.wallet.models.ESUserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "user-creation-processed", groupId = "user-group")
    public void userConsumeInES(String userJson){
        try {
            ESUserModel esUserModel = objectMapper.readValue(userJson, ESUserModel.class);
            SaveUserToES(esUserModel);
            logger.info("User saved in ES: " + esUserModel);
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error while saving user in ES: " + e.getMessage());
        }
    }

    private void SaveUserToES(ESUserModel esUserModel) {
        try{
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index("users_idx") // create index
                    .id(String.valueOf(esUserModel.getWalletId())) // use WalletId as Id
                    .document(esUserModel)
            );
            logger.info("User saved in ES: " + response);
        } catch(Exception e){
            e.printStackTrace();
            logger.error("Error while saving user in ES: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "fund-transfer-processed", groupId = "transaction-group")
    public void transactionConsumeInES(String transactionJson){
        try {
            ESTransactionModel esTransactionModel = objectMapper.readValue(transactionJson, ESTransactionModel.class);
            saveTransactionToES(esTransactionModel);
            logger.info("Transaction saved in ES: " + transactionJson);
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error while saving transaction in ES: " + e.getMessage());
        }
    }

    private void saveTransactionToES(ESTransactionModel esTransactionModel){
        try{
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index("transactions") // create index
                    .id(String.valueOf(esTransactionModel.getTxnId())) // use TransactionId as Id
                    .document(esTransactionModel)
            );
            logger.info("Transaction saved in ES: " + response);
        } catch(Exception e){
            e.printStackTrace();
            logger.error("Error while saving transaction in ES: " + e.getMessage());
        }
    }

}
