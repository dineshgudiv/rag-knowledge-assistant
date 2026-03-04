package com.companyname.ragassistant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name="query_logs")
public class QueryLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, columnDefinition = "TEXT")
  private String question;

  @Column(nullable=false, columnDefinition = "TEXT")
  private String answer;

  @Column(name="citations_json", columnDefinition = "TEXT")
  private String citationsJson;

  @Column(name="best_score")
  private Double bestScore;

  @Column(name="latency_ms")
  private Long latencyMs;

  @Column(name="mode", length = 32)
  private String mode;

  @Column(name="created_at")
  private LocalDateTime createdAt = LocalDateTime.now();
}
