package com.example.emptybot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.Privacy;

@Service
public class MyTelegramBot extends AbilityBot {

    @Autowired
    public MyTelegramBot(Environment env) {
        super(env.getProperty("bottoken"), env.getProperty("botname"));
    }

    public Ability hello() {
        return Ability.builder()
                .name("hello")
                .info("Says hello")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> silent.send("Hello, world!", ctx.chatId()))
                .build();
    }


    @Override
    public long creatorId() {
        return 1L;
    }
}

