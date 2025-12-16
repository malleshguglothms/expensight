package com.gm.expensight.service.impl;

import com.gm.expensight.service.OcrException;
import com.gm.expensight.service.util.PdfToImageConverter;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tesseract OCR Service Tests")
class TesseractOcrServiceTest {
    
    @Mock
    private Tesseract tesseract;
    
    @Mock
    private PdfToImageConverter pdfConverter;
    
    private TesseractOcrService ocrService;
    
    @BeforeEach
    void setUp() {
        // Use package-private constructor for testing
        ocrService = new TesseractOcrService(tesseract, pdfConverter);
    }
    
    @Test
    @DisplayName("Should extract text from image successfully")
    void shouldExtractTextFromImage() throws OcrException, TesseractException {
        // Given
        byte[] imageData = createSampleImage();
        String expectedText = "Sample receipt text";
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn(expectedText);
        
        // When
        String result = ocrService.extractText(imageData);
        
        // Then
        assertThat(result).isEqualTo(expectedText);
        verify(tesseract).doOCR(any(BufferedImage.class));
    }
    
    @Test
    @DisplayName("Should handle TesseractException and wrap in OcrException")
    void shouldHandleTesseractException() throws TesseractException {
        // Given
        byte[] imageData = createSampleImage();
        when(tesseract.doOCR(any(BufferedImage.class)))
                .thenThrow(new TesseractException("OCR failed"));
        
        // When & Then
        assertThatThrownBy(() -> ocrService.extractText(imageData))
                .isInstanceOf(OcrException.class)
                .hasMessageContaining("OCR processing failed");
    }
    
    @Test
    @DisplayName("Should extract text from PDF by converting pages to images")
    void shouldExtractTextFromPdf() throws OcrException, TesseractException, IOException {
        // Given
        byte[] pdfData = new byte[]{1, 2, 3}; // Mock PDF data
        BufferedImage page1 = createSampleBufferedImage();
        BufferedImage page2 = createSampleBufferedImage();
        List<BufferedImage> pages = List.of(page1, page2);
        
        when(pdfConverter.convertToImages(pdfData)).thenReturn(pages);
        when(tesseract.doOCR(page1)).thenReturn("Page 1 text");
        when(tesseract.doOCR(page2)).thenReturn("Page 2 text");
        
        // When
        String result = ocrService.extractTextFromPdf(pdfData);
        
        // Then
        assertThat(result).contains("Page 1 text");
        assertThat(result).contains("Page 2 text");
        verify(pdfConverter).convertToImages(pdfData);
        verify(tesseract, times(2)).doOCR(any(BufferedImage.class));
    }
    
    @Test
    @DisplayName("Should handle PDF conversion failure")
    void shouldHandlePdfConversionFailure() throws IOException {
        // Given
        byte[] pdfData = new byte[]{1, 2, 3};
        when(pdfConverter.convertToImages(pdfData))
                .thenThrow(new IOException("Invalid PDF"));
        
        // When & Then
        assertThatThrownBy(() -> ocrService.extractTextFromPdf(pdfData))
                .isInstanceOf(OcrException.class)
                .hasMessageContaining("Failed to convert PDF to images");
    }
    
    @Test
    @DisplayName("Should throw exception for empty image")
    void shouldHandleEmptyImage() {
        // Given
        byte[] emptyData = new byte[0];
        
        // When & Then
        assertThatThrownBy(() -> ocrService.extractText(emptyData))
                .isInstanceOf(OcrException.class)
                .hasMessageContaining("Failed to read image");
    }
    
    @Test
    @DisplayName("Should return provider name")
    void shouldReturnProviderName() {
        // When
        String providerName = ocrService.getProviderName();
        
        // Then
        assertThat(providerName).isEqualTo("Tesseract");
    }
    
    @Test
    @DisplayName("Should handle null image data")
    void shouldHandleNullImageData() {
        // When & Then
        assertThatThrownBy(() -> ocrService.extractText(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image data cannot be null");
    }
    
    @Test
    @DisplayName("Should handle null PDF data")
    void shouldHandleNullPdfData() {
        // When & Then
        assertThatThrownBy(() -> ocrService.extractTextFromPdf(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PDF data cannot be null");
    }
    
    @Test
    @DisplayName("Should combine text from multiple PDF pages with separator")
    void shouldCombineMultiplePdfPages() throws OcrException, TesseractException, IOException {
        // Given
        byte[] pdfData = new byte[]{1, 2, 3};
        BufferedImage page1 = createSampleBufferedImage();
        BufferedImage page2 = createSampleBufferedImage();
        List<BufferedImage> pages = List.of(page1, page2);
        
        when(pdfConverter.convertToImages(pdfData)).thenReturn(pages);
        when(tesseract.doOCR(page1)).thenReturn("First page");
        when(tesseract.doOCR(page2)).thenReturn("Second page");
        
        // When
        String result = ocrService.extractTextFromPdf(pdfData);
        
        // Then
        assertThat(result).contains("First page");
        assertThat(result).contains("Second page");
        // Should have separator between pages
        assertThat(result.split("\n--- Page \\d+ ---\n").length).isGreaterThan(1);
    }
    
    // Helper method to create a sample image as bytes
    private byte[] createSampleImage() {
        try {
            BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    // Helper method to create a BufferedImage
    private BufferedImage createSampleBufferedImage() {
        return new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    }
}
