package com.companyname.ragassistant.repository;

import com.companyname.ragassistant.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByContentHash(String contentHash);
}
