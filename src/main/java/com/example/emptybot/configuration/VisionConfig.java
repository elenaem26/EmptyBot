package com.example.emptybot.configuration;

import com.google.cloud.vision.v1.ImageAnnotatorClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class VisionConfig {
    @Bean
    public ImageAnnotatorClient client() throws IOException {
        return ImageAnnotatorClient.create();
    }
}

