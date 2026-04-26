package com.elemental.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class DeliveryDateService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String nextDeliveryDate() {
        LocalDate date = LocalDate.now();
        int businessDays = 0;

        while (businessDays < 3) {
            date = date.plusDays(1);
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (dayOfWeek != 6 && dayOfWeek != 7) {
                businessDays++;
            }
        }

        return date.format(FORMATTER);
    }
}
