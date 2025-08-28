package com.example.emptybot.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseUpdateCmd(
        String name,
        BigDecimal price,
        UUID categoryId
) {
}
