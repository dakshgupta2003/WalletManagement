package com.payment.wallet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.wallet.models.ESTransactionModel;
import com.payment.wallet.models.ESUserModel;
import com.payment.wallet.repositories.ESTransactionRepository;
import com.payment.wallet.repositories.ESUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private ESUserRepository esUserRepository;

    @Autowired
    private ESTransactionRepository esTransactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "user-creation-processed", groupId = "user-group")
    public void userConsumeInES(String userJson){
        try {
            ESUserModel esUserModel = objectMapper.readValue(userJson, ESUserModel.class);
            esUserRepository.save(esUserModel);
            logger.info("User saved in ES: " + esUserModel);
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error while saving user in ES: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "fund-transfer-processed", groupId = "transaction-group")
    public void transactionConsumeInES(String transactionJson){
        try {
            ESTransactionModel esTransactionModel = objectMapper.readValue(transactionJson, ESTransactionModel.class);
            esTransactionRepository.save(esTransactionModel);
            logger.info("Transaction saved in ES: " + transactionJson);
        } catch (Exception e){
            e.printStackTrace();
            logger.error("Error while saving transaction in ES: " + e.getMessage());
        }
    }

}
