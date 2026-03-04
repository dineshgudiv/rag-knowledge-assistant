package com.companyname.ragassistant.mapper;

import com.companyname.ragassistant.dto.UploadDocResponse;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public UploadDocResponse toUploadResponse(Long documentId, String name, int chunkCount) {
        return new UploadDocResponse(documentId, chunkCount, name);
    }
}
