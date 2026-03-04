package com.companyname.ragassistant.controller;

import com.companyname.ragassistant.dto.FeedbackRequest;
import com.companyname.ragassistant.exception.NotFoundException;
import com.companyname.ragassistant.model.Feedback;
import com.companyname.ragassistant.repository.FeedbackRepository;
import com.companyname.ragassistant.repository.QueryLogRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/feedback")
public class FeedbackController {
  private final QueryLogRepository queryLogRepository;
  private final FeedbackRepository feedbackRepository;

  public FeedbackController(QueryLogRepository queryLogRepository, FeedbackRepository feedbackRepository) {
    this.queryLogRepository = queryLogRepository;
    this.feedbackRepository = feedbackRepository;
  }

  @PostMapping
  public Feedback submit(@Valid @RequestBody FeedbackRequest req) {
    var log = queryLogRepository.findById(req.queryLogId())
        .orElseThrow(() -> new NotFoundException("query_log not found"));

    Feedback fb = new Feedback();
    fb.setQueryLog(log);
    fb.setHelpful(req.helpful());
    fb.setComment(req.comment());
    return feedbackRepository.save(fb);
  }

  @GetMapping
  public List<Feedback> list(@RequestParam(defaultValue = "50") int limit) {
    int safeLimit = Math.max(1, Math.min(50, limit));
    return feedbackRepository.findAllByOrderByIdDesc(PageRequest.of(0, safeLimit));
  }
}
