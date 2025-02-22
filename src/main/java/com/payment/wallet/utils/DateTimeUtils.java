package com.payment.wallet.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }
} 