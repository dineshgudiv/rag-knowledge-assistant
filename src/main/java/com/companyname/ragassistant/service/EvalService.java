package com.companyname.ragassistant.service;

import com.companyname.ragassistant.dto.AskRequest;
import com.companyname.ragassistant.dto.AskResponse;
import com.companyname.ragassistant.dto.CitationDto;
import com.companyname.ragassistant.exception.NotFoundException;
import com.companyname.ragassistant.model.EvalCase;
import com.companyname.ragassistant.model.EvalRun;
import com.companyname.ragassistant.repository.EvalCaseRepository;
import com.companyname.ragassistant.repository.EvalRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EvalService {
    private final EvalCaseRepository evalCaseRepository;
    private final EvalRunRepository evalRunRepository;
    private final RagService ragService;
    private final ObjectMapper objectMapper;

    public EvalService(EvalCaseRepository evalCaseRepository,
                       EvalRunRepository evalRunRepository,
                       RagService ragService,
                       ObjectMapper objectMapper) {
        this.evalCaseRepository = evalCaseRepository;
        this.evalRunRepository = evalRunRepository;
        this.ragService = ragService;
        this.objectMapper = objectMapper;
    }

    public EvalRun run(Integer topK, Double minScore) {
        List<EvalCase> cases = evalCaseRepository.findAll();
        if (cases.isEmpty()) {
            throw new RuntimeException("No eval_cases found (seed V2 migration).");
        }

        int total = cases.size();
        int passed = 0;
        double sumFaithfulness = 0d;
        double sumRelevancy = 0d;
        int sumRetrievalMs = 0;
        int sumGenerationMs = 0;
        int sumLatencyMs = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (EvalCase c : cases) {
            double caseMinScore = c.getMinScore() == null ? (minScore == null ? 0.2d : minScore) : c.getMinScore();
            AskRequest req = new AskRequest(c.getQuestion(), topK, caseMinScore, null, true);
            long started = System.currentTimeMillis();
            AskResponse r = ragService.ask(req);
            int latency = toInt(r.latencyMs());
            int retrieval = Math.max(0, latency - 1);
            int generation = Math.max(0, latency - retrieval);

            boolean expectedMatch = matchesExpected(c.getExpectedAnswer(), r.answer());
            boolean mustAnswer = c.getMustAnswer() == null || c.getMustAnswer();
            int citationsCount = r.citations() == null ? 0 : r.citations().size();

            boolean ok = expectedMatch;
            if (mustAnswer) {
                ok = ok && !"I don't know.".equals(r.answer()) && citationsCount > 0;
            }

            if (ok) {
                passed++;
            }

            double faithfulness = faithfulness(r, mustAnswer);
            double relevancy = relevancy(c.getQuestion(), r.answer(), mustAnswer);
            sumFaithfulness += faithfulness;
            sumRelevancy += relevancy;
            sumRetrievalMs += retrieval;
            sumGenerationMs += generation;
            sumLatencyMs += latency;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("question", c.getQuestion());
            row.put("expected", c.getExpectedAnswer());
            row.put("got", r.answer());
            row.put("ok", ok);
            row.put("bestScore", r.bestScore());
            row.put("citationsCount", citationsCount);
            row.put("mode", r.mode());
            row.put("faithfulness", round3(faithfulness));
            row.put("relevancy", round3(relevancy));
            row.put("retrievalMs", retrieval);
            row.put("generationMs", generation);
            row.put("latencyMs", latency);
            details.add(row);
        }

        EvalRun run = new EvalRun();
        run.setTotalCases(total);
        run.setPassedCases(passed);
        run.setScore(total == 0 ? 0d : (double) passed / (double) total);
        run.setAvgFaithfulness(total == 0 ? 0d : round3(sumFaithfulness / total));
        run.setAvgRelevancy(total == 0 ? 0d : round3(sumRelevancy / total));
        run.setAvgRetrievalMs(total == 0 ? 0 : sumRetrievalMs / total);
        run.setAvgGenerationMs(total == 0 ? 0 : sumGenerationMs / total);
        run.setAvgLatencyMs(total == 0 ? 0 : sumLatencyMs / total);

        try {
            run.setDetailsJson(objectMapper.writeValueAsString(details));
        } catch (Exception e) {
            run.setDetailsJson("[]");
        }

        return evalRunRepository.save(run);
    }

    public List<EvalRun> listRuns(int limit) {
        int n = Math.max(1, Math.min(limit, 50));
        return evalRunRepository.findAllByOrderByIdDesc(PageRequest.of(0, n));
    }

    public EvalRun getRun(Long id) {
        return evalRunRepository.findById(id).orElseThrow(() -> new NotFoundException("eval_run not found"));
    }

    private boolean matchesExpected(String expected, String got) {
        String expectedNorm = normalize(expected);
        String gotNorm = normalize(got);
        if (expectedNorm.isBlank() || gotNorm.isBlank()) {
            return false;
        }
        if (gotNorm.contains(expectedNorm)) {
            return true;
        }
        return overlapRatio(tokenize(expectedNorm), tokenize(gotNorm)) >= 0.45d;
    }

    private double faithfulness(AskResponse r, boolean mustAnswer) {
        if ("idk".equals(r.mode()) || "blocked".equals(r.mode())) {
            return mustAnswer ? 0d : 1d;
        }
        List<String> answerSentences = splitSentences(r.answer());
        if (answerSentences.isEmpty()) {
            return 0d;
        }
        List<String> citationTexts = r.citations() == null
                ? List.of()
                : r.citations().stream().map(CitationDto::supportSpan).toList();
        if (citationTexts.isEmpty()) {
            return 0d;
        }
        double accum = 0d;
        for (String sentence : answerSentences) {
            double best = 0d;
            for (String cite : citationTexts) {
                best = Math.max(best, overlapRatio(tokenize(sentence), tokenize(cite)));
            }
            accum += best;
        }
        return accum / (double) answerSentences.size();
    }

    private double relevancy(String question, String answer, boolean mustAnswer) {
        if ("I don't know.".equals(answer)) {
            return mustAnswer ? 0d : 1d;
        }
        double overlap = overlapRatio(tokenize(question), tokenize(answer));
        if (mustAnswer) {
            return overlap;
        }
        return Math.min(1d, overlap + 0.1d);
    }

    private double overlapRatio(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0d;
        }
        Set<String> rightSet = new HashSet<>(right);
        int hits = 0;
        for (String token : left) {
            if (rightSet.contains(token)) {
                hits++;
            }
        }
        return Math.min(1d, (double) hits / (double) left.size());
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split(" "));
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("(?<=[.!?])\\s+"));
    }

    private static String normalize(String s) {
        return (s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim());
    }

    private int toInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    private double round3(double value) {
        return Math.round(value * 1000d) / 1000d;
    }
}
