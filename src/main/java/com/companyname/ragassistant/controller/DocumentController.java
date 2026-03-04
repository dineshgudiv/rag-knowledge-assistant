package com.companyname.ragassistant.controller;

import com.companyname.ragassistant.dto.UploadDocResponse;
import com.companyname.ragassistant.model.Chunk;
import com.companyname.ragassistant.model.Document;
import com.companyname.ragassistant.service.AuditService;
import com.companyname.ragassistant.service.DocumentService;
import com.companyname.ragassistant.service.IngestionService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/documents")
public class DocumentController {
    private static final long MAX_UPLOAD_BYTES = 25L * 1024L * 1024L;
    private final IngestionService ingestionService;
    private final DocumentService documentService;
    private final AuditService auditService;

    public DocumentController(IngestionService ingestionService, DocumentService documentService, AuditService auditService) {
        this.ingestionService = ingestionService;
        this.documentService = documentService;
        this.auditService = auditService;
    }

    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadDocResponse upload(@RequestPart("file") @NotNull MultipartFile file) throws Exception {
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large. Max allowed is 25MB");
        }
        String name = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String mime = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        var r = ingestionService.ingest(file.getBytes(), name, mime);
        auditService.logUpload(r.documentId(), r.chunkCount());
        return new UploadDocResponse(r.documentId(), r.chunkCount(), r.name());
    }

    @GetMapping
    public List<Document> list() {
        return documentService.list();
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        documentService.delete(id);
        auditService.logDelete(id);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/{id}/chunks")
    public List<Chunk> chunks(@PathVariable Long id,
                              @RequestParam(defaultValue = "50") int limit) {
        return documentService.chunks(id, limit);
    }
}
