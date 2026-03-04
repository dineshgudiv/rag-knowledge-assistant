package com.companyname.ragassistant.service;

import com.companyname.ragassistant.dto.CitationDto;
import com.companyname.ragassistant.model.Chunk;
import com.companyname.ragassistant.repository.ChunkRepository;
import com.companyname.ragassistant.util.TextChunker;
import com.companyname.ragassistant.util.VectorMath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RetrievalService {
    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "or", "to", "of", "for", "in", "on", "at", "is", "are", "was", "were", "be",
            "this", "that", "with", "as", "by", "it", "from", "not", "if", "but", "into", "about"
    );

    private final ChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final TextChunker textChunker;
    private final double alpha;

    public RetrievalService(ChunkRepository chunkRepository,
                            EmbeddingService embeddingService,
                            TextChunker textChunker,
                            @Value("${app.retrieval.alpha:0.75}") double alpha) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.textChunker = textChunker;
        this.alpha = Math.max(0d, Math.min(1d, alpha));
    }

    public List<CitationDto> retrieve(String question, int topK, List<Long> allowedDocIds, boolean redactPii) {
        String q = question == null ? "" : question.trim();
        if (q.isBlank()) {
            return List.of();
        }

        int cappedTopK = Math.min(5, Math.max(1, topK));
        List<String> qTokens = tokenize(q);
        Set<String> qTokenSet = new HashSet<>(qTokens);
        List<Chunk> candidates = prefilterCandidates(q, allowedDocIds);
        float[] qVec = embeddingService.embed(q);
        Map<Long, Chunk> parentMap = loadParents(candidates);

        List<ScoredChunk> stageOne = new ArrayList<>();
        for (Chunk chunk : candidates) {
            float[] cVec = embeddingService.fromJson(chunk.getEmbeddingJson());
            if (cVec.length == 0) {
                cVec = embeddingService.embed(chunk.getContent());
            }
            double scoreVec = VectorMath.cosineToUnitScore(VectorMath.cosineSimilarity(qVec, cVec));
            double scoreKw = keywordScore(qTokenSet, chunk.getTokens());
            double hybrid = (alpha * scoreVec) + ((1d - alpha) * scoreKw);
            stageOne.add(new ScoredChunk(chunk, scoreVec, scoreKw, hybrid));
        }

        List<ScoredChunk> topHybrid = stageOne.stream()
                .sorted(Comparator.comparing(ScoredChunk::hybrid).reversed())
                .limit(30)
                .toList();

        List<CitationDto> finalScored = new ArrayList<>();
        for (ScoredChunk scoredChunk : topHybrid) {
            Chunk chunk = scoredChunk.chunk();
            double rerank = rerankScore(qTokens, chunk.getContent());
            double finalScore = (0.6d * scoredChunk.hybrid()) + (0.4d * rerank);
            Chunk parent = parentMap.get(chunk.getParentId());
            String sourceText = parent == null ? chunk.getContent() : parent.getContent();
            String supportSpan = textChunker.supportSpan(sourceText, q, 250, 350, redactPii);
            Long documentId = chunk.getDocument() == null ? null : chunk.getDocument().getId();
            String documentName = chunk.getDocument() == null ? null : chunk.getDocument().getName();

            finalScored.add(new CitationDto(
                    chunk.getId(),
                    documentId,
                    documentName,
                    chunk.getParentId(),
                    chunk.getChunkIndex(),
                    finalScore,
                    scoredChunk.scoreVec(),
                    scoredChunk.scoreKw(),
                    null,
                    supportSpan,
                    null,
                    chunk.getStartOffset(),
                    chunk.getEndOffset(),
                    chunk.getPageStart(),
                    chunk.getPageEnd()
            ));
        }

        return finalScored.stream()
                .sorted(Comparator.comparing(CitationDto::score).reversed())
                .limit(cappedTopK)
                .toList();
    }

    private List<Chunk> prefilterCandidates(String q, List<Long> allowedDocIds) {
        String longest = tokenize(q).stream().max(Comparator.comparingInt(String::length)).orElse("");
        List<Long> safeDocIds = allowedDocIds == null ? List.of() : allowedDocIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        boolean docFilter = !safeDocIds.isEmpty();

        if (longest.length() >= 3) {
            List<Chunk> byToken = docFilter
                    ? chunkRepository.findByDocument_IdInAndTokensContainingIgnoreCaseAndChunkIndexGreaterThanEqual(
                    safeDocIds, longest, 0, PageRequest.of(0, 300)).getContent()
                    : chunkRepository.findByTokensContainingIgnoreCaseAndChunkIndexGreaterThanEqual(
                    longest, 0, PageRequest.of(0, 300)).getContent();
            if (!byToken.isEmpty()) {
                return byToken;
            }
        }

        return docFilter
                ? chunkRepository.findByDocument_IdInAndChunkIndexGreaterThanEqual(safeDocIds, 0, PageRequest.of(0, 300)).getContent()
                : chunkRepository.findByChunkIndexGreaterThanEqual(0, PageRequest.of(0, 300)).getContent();
    }

    private Map<Long, Chunk> loadParents(List<Chunk> children) {
        List<Long> parentIds = children.stream()
                .map(Chunk::getParentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Chunk> byId = new HashMap<>();
        for (Chunk parent : chunkRepository.findAllById(parentIds)) {
            byId.put(parent.getId(), parent);
        }
        return byId;
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .replaceAll("\\s+", " ")
                        .trim()
                        .split(" "))
                .stream()
                .filter(token -> token.length() > 1)
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.toList());
    }

    private double keywordScore(Set<String> qTokens, String chunkTokens) {
        if (qTokens.isEmpty() || chunkTokens == null || chunkTokens.isBlank()) {
            return 0d;
        }
        Map<String, Integer> tf = new HashMap<>();
        for (String token : chunkTokens.split(" ")) {
            if (token.isBlank()) {
                continue;
            }
            tf.put(token, tf.getOrDefault(token, 0) + 1);
        }
        double accum = 0d;
        for (String token : qTokens) {
            int count = tf.getOrDefault(token, 0);
            if (count > 0) {
                accum += Math.min(1d, (double) count / 2d);
            }
        }
        return Math.min(1d, accum / (double) qTokens.size());
    }

    private double rerankScore(List<String> qTokens, String content) {
        String cleaned = normalize(content);
        if (cleaned.isBlank() || qTokens.isEmpty()) {
            return 0d;
        }
        double bigram = bigramOverlap(qTokens, tokenize(cleaned));
        int len = cleaned.length();
        double lengthPenalty = 1d;
        if (len < 120) {
            lengthPenalty = 0.75d;
        } else if (len > 1200) {
            lengthPenalty = 0.85d;
        }
        return Math.max(0d, Math.min(1d, bigram * lengthPenalty));
    }

    private double bigramOverlap(List<String> queryTokens, List<String> chunkTokens) {
        if (queryTokens.size() < 2 || chunkTokens.size() < 2) {
            return 0d;
        }
        Set<String> qBigrams = new HashSet<>();
        for (int i = 0; i < queryTokens.size() - 1; i++) {
            qBigrams.add(queryTokens.get(i) + "_" + queryTokens.get(i + 1));
        }
        if (qBigrams.isEmpty()) {
            return 0d;
        }
        Set<String> cBigrams = new HashSet<>();
        for (int i = 0; i < chunkTokens.size() - 1; i++) {
            cBigrams.add(chunkTokens.get(i) + "_" + chunkTokens.get(i + 1));
        }
        int matches = 0;
        for (String qBigram : qBigrams) {
            if (cBigrams.contains(qBigram)) {
                matches++;
            }
        }
        return Math.min(1d, (double) matches / (double) qBigrams.size());
    }

    private String normalize(String content) {
        return content == null ? "" : content.replaceAll("\\s+", " ").trim();
    }

    private record ScoredChunk(Chunk chunk, double scoreVec, double scoreKw, double hybrid) {
    }
}
