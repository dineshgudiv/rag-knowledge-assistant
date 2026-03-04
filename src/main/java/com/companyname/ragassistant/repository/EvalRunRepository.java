package com.companyname.ragassistant.repository;

import com.companyname.ragassistant.model.EvalRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalRunRepository extends JpaRepository<EvalRun, Long> {
    List<EvalRun> findAllByOrderByIdDesc(Pageable pageable);
}
