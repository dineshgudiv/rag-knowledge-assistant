package com.companyname.ragassistant.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Citation {
  private final Long chunkId;
  private final Long documentId;
  private final Integer chunkIndex;
  private final Double score;
  private final String snippet;
}
