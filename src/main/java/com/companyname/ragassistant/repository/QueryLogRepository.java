package com.companyname.ragassistant.repository;

import com.companyname.ragassistant.model.QueryLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryLogRepository extends JpaRepository<QueryLog, Long> {}