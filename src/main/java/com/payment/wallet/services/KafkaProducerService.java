package com.payment.wallet.services;

import com.payment.wallet.models.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final String userTopic = "user-creation";
    private final String transactionTopic = "fund-transfer";

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    public void userPushInKafka(String message){
        kafkaTemplate.send(userTopic,message);
    }

    public void fundsPushInKafka(String message){
        kafkaTemplate.send(transactionTopic,message);
    }


}
