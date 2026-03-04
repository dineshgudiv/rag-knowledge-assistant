package com.companyname.ragassistant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "chunks")
public class Chunk {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="document_id")
  private Document document;

  @Column(name="chunk_index", nullable = false)
  private Integer chunkIndex;

  @Column(name="start_offset")
  private Integer startOffset;

  @Column(name="end_offset")
  private Integer endOffset;

  @Column(name="page_start")
  private Integer pageStart;

  @Column(name="page_end")
  private Integer pageEnd;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name="embedding_json", columnDefinition = "TEXT")
  private String embeddingJson;

  @Column(name="parent_id")
  private Long parentId;

  @Column(name="tokens", columnDefinition = "TEXT")
  private String tokens;
}
