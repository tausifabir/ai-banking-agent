package com.aibankingagent.ai_banking_agent.service;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
public class OcrServiceImpl implements OcrService {
    @Override
    public String extract(MultipartFile file) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata"); // path to trained data

            File temp = File.createTempFile("img", ".png");
            file.transferTo(temp);

            return tesseract.doOCR(temp);

        } catch (Exception e) {
            throw new RuntimeException("OCR failed");
        }
    }
}
