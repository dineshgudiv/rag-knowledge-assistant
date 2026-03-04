package com.companyname.ragassistant.repository;

import com.companyname.ragassistant.model.Feedback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByIdDesc(Pageable pageable);
}
