package com.example.emptybot.service;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.jooq.generated.Tables.COMPANY;


@Service
@Transactional
public class CompanyService {

    @Autowired
    private DSLContext dslContext;

    public void saveCompany(String name) {
        dslContext.insertInto(COMPANY).set(COMPANY.NAME, name).execute();
    }

    public void deleteCompany(String name) {
        dslContext.deleteFrom(COMPANY).where(COMPANY.NAME.eq(name)).execute();
    }
}
