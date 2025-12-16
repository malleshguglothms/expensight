package com.gm.expensight.service.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PdfToImageConverter {
    
    private static final int DEFAULT_DPI = 300; // High DPI for better OCR accuracy
    
    public List<BufferedImage> convertToImages(byte[] pdfData, int dpi) throws IOException {
        if (pdfData == null || pdfData.length == 0) {
            throw new IllegalArgumentException("PDF data cannot be null or empty");
        }
        
        List<BufferedImage> images = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi);
                images.add(image);
            }
        }
        
        return images;
    }
    
    public List<BufferedImage> convertToImages(byte[] pdfData) throws IOException {
        return convertToImages(pdfData, DEFAULT_DPI);
    }
    
    public byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}

