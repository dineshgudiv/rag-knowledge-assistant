package com.companyname.ragassistant.repository;

import com.companyname.ragassistant.model.Chunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {
    Page<Chunk> findByContentContainingIgnoreCase(String needle, Pageable pageable);

    List<Chunk> findByDocumentId(Long documentId);

    List<Chunk> findTop50ByDocument_IdOrderByChunkIndexAsc(Long documentId);

    Page<Chunk> findByChunkIndexGreaterThanEqual(Integer chunkIndex, Pageable pageable);

    Page<Chunk> findByTokensContainingIgnoreCaseAndChunkIndexGreaterThanEqual(String token, Integer chunkIndex, Pageable pageable);

    Page<Chunk> findByDocument_IdInAndChunkIndexGreaterThanEqual(List<Long> documentIds, Integer chunkIndex, Pageable pageable);

    Page<Chunk> findByDocument_IdInAndTokensContainingIgnoreCaseAndChunkIndexGreaterThanEqual(
            List<Long> documentIds,
            String token,
            Integer chunkIndex,
            Pageable pageable
    );

    @Query("select c from Chunk c where c.document.id = :documentId and c.chunkIndex >= 0 order by c.chunkIndex asc")
    List<Chunk> findChildChunksByDocumentId(Long documentId, Pageable pageable);
}
