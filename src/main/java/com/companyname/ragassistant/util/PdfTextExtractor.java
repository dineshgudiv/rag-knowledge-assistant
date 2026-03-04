package com.companyname.ragassistant.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
public class PdfTextExtractor {

    public String extract(byte[] bytes) {
        try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(bytes))) {
            return new PDFTextStripper().getText(doc);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse PDF", e);
        }
    }
}
