package com.companyname.ragassistant.service;

import com.companyname.ragassistant.dto.UploadDocResponse;
import com.companyname.ragassistant.exception.NotFoundException;
import com.companyname.ragassistant.model.Chunk;
import com.companyname.ragassistant.model.Document;
import com.companyname.ragassistant.repository.ChunkRepository;
import com.companyname.ragassistant.repository.DocumentRepository;
import com.companyname.ragassistant.util.HashUtil;
import com.companyname.ragassistant.util.PdfTextExtractor;
import com.companyname.ragassistant.util.TextChunker;
import com.companyname.ragassistant.util.TextSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IngestionService {
    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final int PARENT_SIZE = 2000;
    private static final int PARENT_OVERLAP = 200;
    private static final int CHILD_SIZE = 900;
    private static final int CHILD_OVERLAP = 120;
    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "or", "to", "of", "for", "in", "on", "at", "is", "are", "was", "were", "be",
            "this", "that", "with", "as", "by", "it", "from", "not", "if", "but", "into", "about"
    );

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final boolean embeddingsEnabled;

    public IngestionService(DocumentRepository documentRepository,
                            ChunkRepository chunkRepository,
                            PdfTextExtractor pdfTextExtractor,
                            TextChunker textChunker,
                            EmbeddingService embeddingService,
                            @Value("${app.embeddings.enable:true}") boolean embeddingsEnabled) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.embeddingsEnabled = embeddingsEnabled;
    }

    @Transactional
    public UploadDocResponse ingest(byte[] bytes, String filename, String contentType) {
        String safeName = (filename == null || filename.isBlank()) ? "uploaded.bin" : filename;
        String hash = HashUtil.sha256(bytes);

        Document existing = documentRepository.findByContentHash(hash).orElse(null);
        if (existing != null) {
            int existingChunks = chunkRepository.findByDocumentId(existing.getId()).size();
            return new UploadDocResponse(existing.getId(), existingChunks, existing.getName());
        }

        String original = TextSanitizer.sanitizeForPgText(extractText(bytes, safeName));
        String text = textChunker.normalizeExtractedText(original);
        if (original.length() > TextChunker.MAX_DOC_CHARS) {
            log.warn("event=ingest_truncated reason=max_doc_chars name={} originalChars={} maxChars={}",
                    safeName, original.length(), TextChunker.MAX_DOC_CHARS);
        }
        List<TextChunker.Segment> parentSegments = textChunker.segments(text, PARENT_SIZE, PARENT_OVERLAP);
        if (parentSegments.isEmpty()) {
            parentSegments = List.of(new TextChunker.Segment(0, text.length(), text));
        }

        Document doc = new Document();
        doc.setName(safeName);
        doc.setMimeType(contentType);
        doc.setContentHash(hash);
        Document saved = documentRepository.save(doc);

        List<Chunk> childChunks = new ArrayList<>();
        int childIndex = 0;
        for (TextChunker.Segment parentSegment : parentSegments) {
            Chunk parentChunk = new Chunk();
            parentChunk.setDocument(saved);
            parentChunk.setChunkIndex(-1);
            parentChunk.setStartOffset(parentSegment.startOffset());
            parentChunk.setEndOffset(parentSegment.endOffset());
            String parentContent = TextSanitizer.sanitizeForPgText(parentSegment.text());
            parentChunk.setContent(parentContent);
            parentChunk.setTokens(TextSanitizer.sanitizeForPgText(normalizeTokens(parentContent)));
            parentChunk.setPageStart(1);
            parentChunk.setPageEnd(1);
            Chunk savedParent = chunkRepository.save(parentChunk);

            List<TextChunker.Segment> childSegments = textChunker.segments(parentSegment.text(), CHILD_SIZE, CHILD_OVERLAP);
            for (TextChunker.Segment childSegment : childSegments) {
                int absStart = parentSegment.startOffset() + childSegment.startOffset();
                int absEnd = parentSegment.startOffset() + childSegment.endOffset();

                Chunk childChunk = new Chunk();
                childChunk.setDocument(saved);
                childChunk.setParentId(savedParent.getId());
                childChunk.setChunkIndex(childIndex++);
                childChunk.setStartOffset(absStart);
                childChunk.setEndOffset(absEnd);
                String childContent = TextSanitizer.sanitizeForPgText(childSegment.text());
                String childTokens = TextSanitizer.sanitizeForPgText(normalizeTokens(childContent));
                childChunk.setContent(childContent);
                childChunk.setTokens(childTokens);
                childChunk.setPageStart(1);
                childChunk.setPageEnd(1);
                if (embeddingsEnabled) {
                    childChunk.setEmbeddingJson(embeddingService.toJson(embeddingService.embed(childContent)));
                }
                childChunks.add(childChunk);
            }
        }
        chunkRepository.saveAll(childChunks);
        if (childChunks.size() >= TextChunker.MAX_CHUNKS) {
            log.warn("event=ingest_chunk_limit_reached name={} maxChunks={}", safeName, TextChunker.MAX_CHUNKS);
        }

        return new UploadDocResponse(saved.getId(), childChunks.size(), saved.getName());
    }

    @Transactional
    public int rebuildEmbeddings(Long documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("document not found: " + documentId));
        List<Chunk> chunks = chunkRepository.findByDocumentId(doc.getId());
        for (Chunk chunk : chunks) {
            if (chunk.getChunkIndex() < 0) {
                continue;
            }
            String cleanContent = TextSanitizer.sanitizeForPgText(chunk.getContent());
            chunk.setContent(cleanContent);
            chunk.setEmbeddingJson(embeddingService.toJson(embeddingService.embed(cleanContent)));
            chunk.setTokens(TextSanitizer.sanitizeForPgText(normalizeTokens(cleanContent)));
        }
        chunkRepository.saveAll(chunks);
        return chunks.size();
    }

    private String extractText(byte[] bytes, String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return pdfTextExtractor.extract(bytes);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String normalizeTokens(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .lines()
                .flatMap(line -> List.of(line.split(" ")).stream())
                .filter(token -> token.length() > 1)
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.joining(" "));
    }
}
