package com.companyname.ragassistant.service;

import com.companyname.ragassistant.dto.UploadDocResponse;
import com.companyname.ragassistant.exception.NotFoundException;
import com.companyname.ragassistant.model.Chunk;
import com.companyname.ragassistant.model.Document;
import com.companyname.ragassistant.repository.ChunkRepository;
import com.companyname.ragassistant.repository.DocumentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentService {

    private final IngestionService ingestionService;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    public DocumentService(IngestionService ingestionService, DocumentRepository documentRepository, ChunkRepository chunkRepository) {
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    public UploadDocResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        try {
            return ingestionService.ingest(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }
    }

    public List<Document> list() {
        return documentRepository.findAll();
    }

    public int rebuildEmbeddings(Long documentId) {
        return ingestionService.rebuildEmbeddings(documentId);
    }

    @Transactional
    public void delete(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new NotFoundException("document not found");
        }
        documentRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Chunk> chunks(Long documentId, int limit) {
        if (!documentRepository.existsById(documentId)) {
            throw new NotFoundException("document not found");
        }
        int safeLimit = Math.max(1, Math.min(50, limit));
        return chunkRepository.findChildChunksByDocumentId(documentId, PageRequest.of(0, safeLimit));
    }
}
