package com.companyname.ragassistant.service;

import com.companyname.ragassistant.dto.CitationDto;
import com.companyname.ragassistant.model.AuditLog;
import com.companyname.ragassistant.repository.AuditLogRepository;
import com.companyname.ragassistant.util.ActorUtil;
import com.companyname.ragassistant.util.RequestIdUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> list(int limit) {
        int safeLimit = Math.max(1, Math.min(50, limit));
        return auditLogRepository.findAllByOrderByIdDesc(PageRequest.of(0, safeLimit));
    }

    public void logUpload(Long documentId, int chunkCount) {
        AuditLog log = base("UPLOAD", "/v1/documents/upload");
        log.setDocIds(documentId == null ? null : String.valueOf(documentId));
        log.setChunkIds(chunkCount > 0 ? String.valueOf(chunkCount) : null);
        auditLogRepository.save(log);
    }

    public void logDelete(Long documentId) {
        AuditLog log = base("DELETE", "/v1/documents/{id}");
        log.setDocIds(documentId == null ? null : String.valueOf(documentId));
        auditLogRepository.save(log);
    }

    public void logEval(Integer latencyMs) {
        AuditLog log = base("EVAL", "/v1/eval/run");
        log.setLatencyMs(latencyMs);
        auditLogRepository.save(log);
    }

    public void logAsk(String question,
                       List<CitationDto> citations,
                       Double bestScore,
                       String mode,
                       Integer latencyMs,
                       Integer retrievalMs,
                       Integer generationMs,
                       boolean blocked,
                       String blockReason) {
        AuditLog log = base("ASK", "/v1/chat/ask");
        log.setQuestion(question);
        log.setDocIds(joinIds(citations.stream().map(CitationDto::documentId).toList()));
        log.setChunkIds(joinIds(citations.stream().map(CitationDto::chunkId).toList()));
        log.setBestScore(bestScore);
        log.setMode(mode);
        log.setLatencyMs(latencyMs);
        log.setRetrievalMs(retrievalMs);
        log.setGenerationMs(generationMs);
        log.setBlocked(blocked);
        log.setBlockReason(blockReason);
        auditLogRepository.save(log);
    }

    private AuditLog base(String action, String route) {
        AuditLog log = new AuditLog();
        log.setRequestId(RequestIdUtil.current());
        log.setAction(action);
        log.setRoute(route);
        log.setActor(ActorUtil.current());
        return log;
    }

    private String joinIds(Collection<Long> ids) {
        String joined = ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.joining(","));
        return joined.isBlank() ? null : joined;
    }
}
