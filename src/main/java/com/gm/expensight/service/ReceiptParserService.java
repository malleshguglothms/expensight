package com.gm.expensight.service;

import com.gm.expensight.domain.model.Receipt;
import com.gm.expensight.exception.LlmException;
import com.gm.expensight.service.dto.ReceiptParsingResult;

public interface ReceiptParserService {
    
    ReceiptParsingResult parseReceipt(String ocrText) throws LlmException;
    
    Receipt applyParsingResult(Receipt receipt, ReceiptParsingResult parsingResult);
}

