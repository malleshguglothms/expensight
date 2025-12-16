package com.gm.expensight.service.impl;

import com.gm.expensight.exception.ValidationException;
import com.gm.expensight.service.PromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PromptServiceImpl implements PromptService {
    
    private static final String RECEIPT_PARSING_PROMPT_TEMPLATE = """
        You are an expert financial data extraction system. Extract structured receipt data from the OCR text below.
        
        CRITICAL: Return ONLY valid JSON. No markdown, no code blocks, no explanations. Just the JSON object.
        
        Required fields:
        - merchantName: Store/merchant name (string, required)
        - totalAmount: Total paid amount (number, required, no currency symbols)
        - receiptDate: Date in YYYY-MM-DD format (string, required)
        - taxAmount: Tax amount if visible (number or null)
        - currency: Currency code like INR, USD, EUR (string, default: INR)
        - items: Array of purchased items, each with:
          - itemName: Item description (string, required)
          - quantity: Quantity (number, default: 1)
          - price: Price per unit (number, required)
        
        IMPORTANT: If the OCR text does NOT contain receipt-related information (e.g., it's a diagram, document, or unrelated content), return merchantName as "NOT_A_RECEIPT" to indicate the file is not a receipt.
        
        Extraction rules:
        1. Dates: Convert to YYYY-MM-DD. If only day/month found, use current year.
        2. Amounts: Extract as numbers only (remove currency symbols, commas).
        3. Items: If same item appears multiple times, combine with total quantity.
        4. Tax: Set to null if not explicitly shown.
        5. Currency: CRITICAL - Infer from symbols:
           - ₹ (rupee symbol) = INR (Indian Rupee) - NOT JPY
           - $ = USD (US Dollar)
           - € = EUR (Euro)
           - If you see "JPY" or "¥" in Indian context (GST, CGST, SGST, or amounts in hundreds/thousands), it's likely INR misread
           - Default to INR if currency is unclear or for Indian receipts
        6. Merchant: Extract the store/business name clearly visible. If no receipt data found, set to "NOT_A_RECEIPT".
        7. Missing data: Make reasonable inferences, but prioritize accuracy.
        8. Indian Receipts: If you see GST, CGST, SGST, or amounts typical of Indian currency (hundreds to lakhs), assume INR currency.
        9. Non-receipt detection: If the content is clearly not a receipt (diagrams, system architecture, documents, etc.), set merchantName to "NOT_A_RECEIPT".
        
        OCR Text:
        %s
        
        Return JSON only (no markdown, no code blocks):
        """;
    
    @Override
    public String buildReceiptParsingPrompt(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            throw new ValidationException("OCR text cannot be null or empty");
        }
        
        return String.format(RECEIPT_PARSING_PROMPT_TEMPLATE, ocrText.trim());
    }
}

