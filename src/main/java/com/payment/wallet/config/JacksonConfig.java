package com.payment.wallet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // purpose of this class is to configure the Jackson ObjectMapper to serialize and deserialize dates in a specific format
    // this is to ensure that the dates are serialized in the format "2024-01-01T00:00:00.000Z" and deserialized in the format "2024-01-01T00:00:00.000Z"
    // Prevents dates from being written as arrays/timestamps [2024, 3, 15, 10, 30]
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
} 