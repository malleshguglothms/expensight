package com.gm.expensight.service.impl;

import com.gm.expensight.exception.ValidationException;
import com.gm.expensight.service.PromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PromptServiceImpl implements PromptService {
    
    private static final String RECEIPT_PARSING_PROMPT_TEMPLATE = """
        You are a deterministic financial receipt data extraction system.
        
        Your task is to extract structured receipt information from OCR text.
        You must behave like a backend service, not a conversational assistant.
        
        OUTPUT CONTRACT (STRICT)
        CRITICAL:
        - Return ONLY valid JSON
        - Do NOT include markdown, explanations, comments, or extra text
        - The response MUST be directly JSON-parseable
        
        OUTPUT SCHEMA
        {
          "merchantName": string,                // required
          "totalAmount": number,                 // required, numeric only
          "receiptDate": string,                 // required, YYYY-MM-DD
          "taxAmount": number | null,             // optional
          "currency": string,                    // ISO code, default "INR"
          "items": [
            {
              "itemName": string,                // required
              "quantity": number,                // default 1
              "price": number                    // required, unit price
            }
          ]
        }
        
        NON-RECEIPT DETECTION (MANDATORY)
        If the OCR text does NOT represent a receipt (e.g., diagrams, articles,
        system designs, forms, or unrelated documents):
        - Set "merchantName" to "NOT_A_RECEIPT"
        - Set all other fields to null or empty values
        - Return JSON only
        
        EXTRACTION RULES (HARD CONSTRAINTS)
        1. Merchant Name
           - Extract the store or business name clearly visible on the receipt
           - If none found, use the most prominent header text
           - If still unclear, set "merchantName" to "UNKNOWN_MERCHANT"
        
        2. Total Amount
           - Extract the FINAL amount paid by the customer
           - Prefer labels like: TOTAL, GRAND TOTAL, AMOUNT PAID, NET PAYABLE
           - Ignore subtotals, discounts, and intermediate totals
           - Extract as number only (remove currency symbols, commas)
        
        3. Date
           - Convert to YYYY-MM-DD
           - If year is missing, use 2025
           - If multiple dates exist, choose the transaction date
           - Dates must not be in the future (allow up to 1 day buffer for timezone differences)
        
        4. Tax
           - Extract only if explicitly shown (GST, CGST, SGST, VAT, TAX)
           - Otherwise set to null
        
        5. Currency
           - Infer from symbols:
             - ₹ → INR (Indian Rupee)
             - $ → USD (US Dollar)
             - € → EUR (Euro)
             - £ → GBP (British Pound)
             - ¥ → JPY (Japanese Yen) - BUT if seen with GST/CGST/SGST or Indian context, treat as INR misread
           - If GST/CGST/SGST is present or values are typical of Indian receipts, default to INR
           - If currency is ambiguous, default to INR
        
        ITEM EXTRACTION RULES
        - Extract each distinct purchased item if item-level data is present
        - itemName must be the ACTUAL product description visible on the receipt
        - Clean common OCR errors (spacing, casing, misread characters)
        - Preserve meaningful details (brand, size, variant)
        - Combine duplicate items and sum their quantities
        - DO NOT use generic placeholders like "Item 1", "Product", "Unknown"
        - If item names are unreadable with low confidence, return an empty array
        
        INFERENCE & SAFETY RULES
        - Prefer accuracy over completeness
        - Infer missing values ONLY when confidence is high
        - Never fabricate merchants, items, or amounts
        - If a required field cannot be determined safely, use the safest value
          (null, empty array, or UNKNOWN_MERCHANT)
        
        OCR INPUT
        %s
        """;
    
    @Override
    public String buildReceiptParsingPrompt(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            throw new ValidationException("OCR text cannot be null or empty");
        }
        
        return String.format(RECEIPT_PARSING_PROMPT_TEMPLATE, ocrText.trim());
    }
}

