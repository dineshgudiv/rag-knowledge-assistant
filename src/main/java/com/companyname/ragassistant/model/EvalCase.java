package com.companyname.ragassistant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name="eval_cases")
public class EvalCase {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable=false, columnDefinition = "TEXT")
  private String question;

  @Column(name="expected_answer", nullable=false, columnDefinition = "TEXT")
  private String expectedAnswer;

  @Column(nullable = false)
  private String category = "general";

  @Column(name="must_answer", nullable = false)
  private Boolean mustAnswer = true;

  @Column(name="min_score", nullable = false)
  private Double minScore = 0.2;

  @Column(name="created_at")
  private LocalDateTime createdAt = LocalDateTime.now();
}
