package com.payment.wallet.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;

@Data
@Document(indexName = "users")
public class ESUserModel {
    
    
    @Id
    @Field(type = FieldType.Long)
    private Long walletId;
    
    @Field(type = FieldType.Double)
    private Double balance;
    
    @Field(type = FieldType.Keyword)
    private String userPhone;
}
