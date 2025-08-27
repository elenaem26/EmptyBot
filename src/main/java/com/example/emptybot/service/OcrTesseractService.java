package com.example.emptybot.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.ITesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

@Service
public class OcrTesseractService {

    private final ITesseract tesseract;

    public OcrTesseractService(
            @Value("${ocr.tessdataPath}") String tessdataPath,
            @Value("${ocr.lang}") String lang,
            @Value("${ocr.psm}") int psm
    ) {
        this.tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(lang);
        tesseract.setOcrEngineMode(1);
        tesseract.setPageSegMode(psm);
    }

    public String read(byte[] imageBytes) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        return tesseract.doOCR(img);
    }

    // для кропов с ценами
    public String ocrNumberLine(byte[] imageBytes) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        tesseract.setPageSegMode(7); // одна строка
        tesseract.setTessVariable("tessedit_char_whitelist", "0123456789.,₾GELლ:/-");
        try {
            return tesseract.doOCR(img);
        } finally {
            tesseract.setPageSegMode(6);
            tesseract.setTessVariable("tessedit_char_whitelist", null);
        }
    }
}

