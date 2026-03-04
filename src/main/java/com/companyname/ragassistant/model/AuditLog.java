package com.companyname.ragassistant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(nullable = false, length = 128)
    private String route;

    @Column(nullable = false, length = 128)
    private String actor = "anonymous";

    @Column(nullable = false, length = 64)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(name = "doc_ids", columnDefinition = "TEXT")
    private String docIds;

    @Column(name = "chunk_ids", columnDefinition = "TEXT")
    private String chunkIds;

    @Column(name = "best_score")
    private Double bestScore;

    @Column(length = 32)
    private String mode;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "retrieval_ms")
    private Integer retrievalMs;

    @Column(name = "generation_ms")
    private Integer generationMs;

    @Column(nullable = false)
    private Boolean blocked = false;

    @Column(name = "block_reason", length = 128)
    private String blockReason;
}
