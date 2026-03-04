package com.companyname.ragassistant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name="eval_runs")
public class EvalRun {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name="total_cases", nullable=false)
  private Integer totalCases;

  @Column(name="passed_cases", nullable=false)
  private Integer passedCases;

  @Column(nullable=false)
  private Double score;

  @Column(name="details_json", columnDefinition = "TEXT")
  private String detailsJson;

  @Column(name="avg_faithfulness", nullable = false)
  private Double avgFaithfulness = 0d;

  @Column(name="avg_relevancy", nullable = false)
  private Double avgRelevancy = 0d;

  @Column(name="avg_retrieval_ms", nullable = false)
  private Integer avgRetrievalMs = 0;

  @Column(name="avg_generation_ms", nullable = false)
  private Integer avgGenerationMs = 0;

  @Column(name="avg_latency_ms", nullable = false)
  private Integer avgLatencyMs = 0;

  @Column(name="created_at")
  private LocalDateTime createdAt = LocalDateTime.now();
}
