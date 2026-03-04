package com.companyname.ragassistant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name="feedback")
public class Feedback {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="query_log_id")
  private QueryLog queryLog;

  @Column(nullable=false)
  private Boolean helpful;

  @Column(columnDefinition = "TEXT")
  private String comment;

  @Column(name="created_at")
  private LocalDateTime createdAt = LocalDateTime.now();
}
