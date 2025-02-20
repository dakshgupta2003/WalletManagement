package com.payment.wallet.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;

@Data
@Document(indexName = "users_idx") // --> don't need if we are using ES rest client
public class ESUserModel {
    @Id
    @Field(type = FieldType.Long)
    private Long walletId;
    
    @Field(type = FieldType.Double)
    private Double balance;
    
    @Field(type = FieldType.Text) // for fuzzy search we require Text field (not keyword)
    private String userPhone;
}
