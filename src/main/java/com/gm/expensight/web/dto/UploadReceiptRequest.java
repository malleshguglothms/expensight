package com.gm.expensight.web.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadReceiptRequest {
    private MultipartFile file;
}

