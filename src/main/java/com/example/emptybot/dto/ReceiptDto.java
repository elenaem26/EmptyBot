package com.example.emptybot.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReceiptDto(
        List<ItemDto> items,
        BigDecimal sum,
        String date
) {
    public record ItemDto(
            String nameParsed,
            String name,
            BigDecimal price,
            String currency,
            UUID categoryId,
            String categorySuggest
    ) {}
}
