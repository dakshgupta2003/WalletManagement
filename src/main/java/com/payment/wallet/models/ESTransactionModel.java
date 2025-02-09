package com.payment.wallet.models;

import com.payment.wallet.Enum.StatusEnum;
import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "transactions")
public class ESTransactionModel {

    @Id
    @Field(type = FieldType.Keyword)
    private String txnId;

    @Field(type = FieldType.Double)
    private Double amount;

    @Field(type = FieldType.Long)
    private Long payerWalletId;

    @Field(type = FieldType.Long)
    private Long payeeWalletId;

    @Field(type = FieldType.Keyword)
    private StatusEnum status;

    @Field(type = FieldType.Date)
    private LocalDateTime timeStamp;
}
