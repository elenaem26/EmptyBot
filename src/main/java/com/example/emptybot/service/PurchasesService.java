package com.example.emptybot.service;

import com.example.emptybot.dto.PurchaseCreateCmd;
import com.example.emptybot.dto.PurchaseUpdateCmd;
import com.example.emptybot.dto.ReceiptDto;
import com.example.jooq.generated.tables.records.PurchasesRecord;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.jooq.generated.Tables.PURCHASES;

@Service
@Transactional
public class PurchasesService {

    @Autowired
    private DSLContext dsl;

    public PurchasesRecord create(PurchaseCreateCmd cmd) {
        UUID id = UUID.randomUUID();
        PurchasesRecord r = dsl.newRecord(PURCHASES);
        r.setId(id);
        r.setNameParsed(cmd.nameParsed());
        r.setName(cmd.name());
        r.setPrice(cmd.price());
        r.setCurrency(cmd.currency());
        r.setPurchasedAt(cmd.purchasedAt());
        r.setStatus(cmd.status());
        r.setCategoryId(cmd.categoryId());
        r.setCheckId(cmd.checkId());
        r.store();
        return r;
    }

    public void create(ReceiptDto items) {
        for(ReceiptDto.ItemDto item : items.items()) {
            create(item);
        }
    }

    public PurchasesRecord create(ReceiptDto.ItemDto item) {
        UUID id = UUID.randomUUID();
        PurchasesRecord r = dsl.newRecord(PURCHASES);
        r.setId(id);
        r.setNameParsed(item.nameParsed());
        r.setName(item.name());
        r.setPrice(item.price());
        r.setCurrency(item.currency() == null ? "GEL" : item.currency());
        r.setPurchasedAt(Instant.now());
        r.setCategoryId(item.categoryId());
        r.store();
        return r;
    }

    public PurchasesRecord updatePurchase(UUID id, PurchaseUpdateCmd cmd) {
        Map<Field<?>, Object> updates = new HashMap<>();
        if (cmd.name() != null) updates.put(PURCHASES.NAME, cmd.name());
        if (cmd.price() != null) updates.put(PURCHASES.PRICE, cmd.price());
        if (cmd.categoryId() != null) updates.put(PURCHASES.CATEGORY_ID, cmd.categoryId());

        if (updates.isEmpty()) {
            return dsl.fetchOne(PURCHASES, PURCHASES.ID.eq(id));
        }

        return dsl.update(PURCHASES)
                .set(updates)
                .where(PURCHASES.ID.eq(id))
                .returning()
                .fetchOne();

    }

    public void deletePurchase(UUID id) {
        dsl.deleteFrom(PURCHASES)
                .where(PURCHASES.ID.eq(id))
                .execute();
    }


    public List<PurchasesRecord> find() {
        return dsl.selectFrom(PURCHASES).fetch();
    }
}

