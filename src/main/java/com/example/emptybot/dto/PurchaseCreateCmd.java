package com.example.emptybot.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PurchaseCreateCmd(
        String nameParsed,
        String name,
        BigDecimal price,
        String currency,
        String status,
        Instant purchasedAt,
        UUID categoryId,
        UUID checkId
) {
}
