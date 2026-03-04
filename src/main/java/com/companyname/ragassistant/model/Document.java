package com.companyname.ragassistant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "documents")
public class Document {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(name="mime_type")
  private String mimeType;

  @Column(name="content_hash")
  private String contentHash;

  @Column(name="created_at")
  private LocalDateTime createdAt = LocalDateTime.now();
}
