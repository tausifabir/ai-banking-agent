package com.aibankingagent.ai_banking_agent.service;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfServiceImpl implements PdfService {
    @Override
    public String extract(MultipartFile file) {

        try (PDDocument doc = PDDocument.load(file.getInputStream())) {

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);

        } catch (Exception e) {
            throw new RuntimeException("PDF processing failed");
        }
    }
}
