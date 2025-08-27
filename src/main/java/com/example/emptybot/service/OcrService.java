package com.example.emptybot.service;


import com.google.cloud.vision.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OcrService {

    @Autowired
    private ImageAnnotatorClient client;

    public String ocrBytes(byte[] bytes) throws Exception {
        var img = Image.newBuilder()
                .setContent(com.google.protobuf.ByteString.copyFrom(bytes))
                .build();

        var ctx = ImageContext.newBuilder()
                .addLanguageHints("ka") // грузинский
                .addLanguageHints("en") // часто встречается вместе
                .build();

        var feature = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();

        var req = AnnotateImageRequest.newBuilder()
                .setImage(img)
                .addFeatures(feature)
                .setImageContext(ctx)
                .build();

        var resp = client.batchAnnotateImages(java.util.List.of(req)).getResponses(0);
        var full = resp.getFullTextAnnotation(); // предпочтительнее для чеков
        if (resp.hasError()) throw new RuntimeException(resp.getError().getMessage());
        return full.getText(); // весь текст одним блоком
    }
}
