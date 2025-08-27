package com.example.emptybot.configuration;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
public class VisionConfig {

    private final Logger logger = LoggerFactory.getLogger(VisionConfig.class);

    @Bean
    public ImageAnnotatorClient imageAnnotatorClient() throws Exception {
        String b64 = System.getenv("GCV_KEY");
        logger.info("GCV_KEY: {}", b64 == null ? "null" : "not null");

        ImageAnnotatorSettings.Builder builder = ImageAnnotatorSettings.newBuilder();

        if (b64 != null && !b64.isBlank()) {
            byte[] json = Base64.getDecoder().decode(b64);
            var creds = ServiceAccountCredentials.fromStream(new ByteArrayInputStream(json));
            builder.setCredentialsProvider(() -> creds);
        }

        return ImageAnnotatorClient.create(builder.build());
    }
}

