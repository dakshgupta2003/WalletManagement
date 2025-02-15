package com.payment.wallet.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.antlr.v4.runtime.misc.NotNull;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserModel {
    @Id
    @Field(type = FieldType.Long)
    private Long walletId;
    
    @NotNull
    @Field(type = FieldType.Double)
    private Double balance;
    
    @NotNull
    @Column(unique = true)
    @Field(type = FieldType.Text) // for fuzzy search we require Text field (not keyword)
    private String userPhone;
}
