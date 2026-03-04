package com.companyname.ragassistant.service;

import com.companyname.ragassistant.dto.AskRequest;
import com.companyname.ragassistant.dto.AskResponse;
import com.companyname.ragassistant.dto.CitationDto;
import com.companyname.ragassistant.exception.DependencyUnavailableException;
import com.companyname.ragassistant.model.QueryLog;
import com.companyname.ragassistant.repository.QueryLogRepository;
import com.companyname.ragassistant.util.PromptInjectionDetector;
import com.companyname.ragassistant.util.RequestIdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RagService {
    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final Set<String> STOPWORDS = Set.of(
            "the", "a", "an", "and", "or", "to", "of", "for", "in", "on", "at", "is", "are", "was", "were", "be",
            "this", "that", "with", "as", "by", "it", "from", "not", "if", "but", "into", "about"
    );
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE = Pattern.compile("(?:\\+?\\d[\\d\\s().-]{8,}\\d)");
    private static final Pattern DOB = Pattern.compile("\\b(?:dob|date of birth)\\b|\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b", Pattern.CASE_INSENSITIVE);

    private final RetrievalService retrievalService;
    private final QueryLogRepository queryLogRepository;
    private final AuditService auditService;
    private final PromptInjectionDetector promptInjectionDetector;
    private final ObjectMapper objectMapper;
    private final int defaultTopK;
    private final double defaultMinScore;

    public RagService(RetrievalService retrievalService,
                      QueryLogRepository queryLogRepository,
                      AuditService auditService,
                      PromptInjectionDetector promptInjectionDetector,
                      ObjectMapper objectMapper,
                      @Value("${app.rag.defaultTopK:3}") int defaultTopK,
                      @Value("${app.rag.defaultMinScore:0.2}") double defaultMinScore) {
        this.retrievalService = retrievalService;
        this.queryLogRepository = queryLogRepository;
        this.auditService = auditService;
        this.promptInjectionDetector = promptInjectionDetector;
        this.objectMapper = objectMapper;
        this.defaultTopK = defaultTopK;
        this.defaultMinScore = defaultMinScore;
    }

    public AskResponse ask(AskRequest request) {
        long start = System.currentTimeMillis();
        int topK = request.topK() == null || request.topK() <= 0 ? defaultTopK : Math.min(5, request.topK());
        double minScore = request.minScore() == null ? defaultMinScore : request.minScore();
        boolean redactPii = request.redactPii() == null || request.redactPii();
        String blockReason = promptInjectionDetector.detectReason(request.question());

        if (blockReason != null) {
            long latency = System.currentTimeMillis() - start;
            return persistAndAudit(request, "I don't know.", List.of(), 0d, "blocked", latency, 0, 0, true, blockReason);
        }

        long retrievalStart = System.currentTimeMillis();
        List<CitationDto> citations;
        try {
            citations = retrievalService.retrieve(request.question(), topK, request.allowedDocIds(), redactPii);
        } catch (RuntimeException ex) {
            throw new DependencyUnavailableException("Retrieval dependency failed", ex);
        }
        long retrievalMs = System.currentTimeMillis() - retrievalStart;

        long generationStart = System.currentTimeMillis();
        double bestScore = citations.isEmpty() ? 0d : citations.get(0).score();

        String answer;
        String mode;
        List<CitationDto> finalCitations;
        if (citations.isEmpty() || bestScore < minScore) {
            answer = "I don't know.";
            mode = "extractive";
            finalCitations = List.of();
            bestScore = 0d;
        } else {
            finalCitations = citations;
            answer = buildEvidenceOnlyAnswer(request.question(), finalCitations);
            mode = finalCitations.size() > 1 ? "extractive_multi" : "extractive";
        }
        long generationMs = System.currentTimeMillis() - generationStart;

        if (!"I don't know.".equals(answer) && !isSupportedByQuotes(answer, finalCitations)) {
            answer = fallbackAnswer(finalCitations);
        }

        long latency = System.currentTimeMillis() - start;
        return persistAndAudit(request, answer, finalCitations, bestScore, mode, latency, toInt(retrievalMs), toInt(generationMs), false, null);
    }

    private AskResponse persistAndAudit(AskRequest request,
                                        String answer,
                                        List<CitationDto> finalCitations,
                                        double bestScore,
                                        String mode,
                                        long latency,
                                        int retrievalMs,
                                        int generationMs,
                                        boolean blocked,
                                        String blockReason) {
        QueryLog queryLog = new QueryLog();
        queryLog.setQuestion(request.question());
        queryLog.setAnswer(answer);
        queryLog.setBestScore(bestScore);
        queryLog.setLatencyMs(latency);
        queryLog.setCitationsJson(toJson(finalCitations));
        queryLog.setMode(mode);
        QueryLog saved = queryLogRepository.save(queryLog);

        log.info("event=ask request_id={} queryLogId={} bestScore={} mode={} retrievalMs={} generationMs={} latencyMs={}",
                RequestIdUtil.current(), saved.getId(), bestScore, mode, retrievalMs, generationMs, latency);

        auditService.logAsk(
                request.question(),
                finalCitations,
                bestScore,
                mode,
                toInt(latency),
                retrievalMs,
                generationMs,
                blocked,
                blockReason
        );

        return new AskResponse(answer, finalCitations, bestScore, saved.getId(), latency, mode, RequestIdUtil.current());
    }

    private String toJson(List<CitationDto> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String buildEvidenceOnlyAnswer(String question, List<CitationDto> citations) {
        List<String> queryTokens = tokenize(question);
        List<String> fragments = new ArrayList<>();
        int citationNumber = 1;

        for (CitationDto citation : citations) {
            String span = citation.supportSpan();
            for (String sentence : splitSentences(span)) {
                String trimmed = sentence.trim();
                if (trimmed.isBlank()) {
                    continue;
                }
                if (containsSensitive(trimmed) && !queryAsksSensitive(question)) {
                    continue;
                }
                if (!queryTokens.isEmpty() && overlaps(queryTokens, tokenize(trimmed)) == 0) {
                    continue;
                }
                fragments.add(limit(trimmed, 220) + " [" + citationNumber + "]");
                break;
            }
            citationNumber++;
            if (joinedLength(fragments) >= 600 || fragments.size() >= 3) {
                break;
            }
        }

        if (fragments.isEmpty()) {
            return fallbackAnswer(citations);
        }
        return limit(String.join(" ", fragments), 600);
    }

    private String fallbackAnswer(List<CitationDto> citations) {
        if (citations.isEmpty()) {
            return "I don't know.";
        }
        String span = limit(citations.get(0).supportSpan(), 560);
        return span + " [1]";
    }

    private boolean queryAsksSensitive(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        return q.contains("dob") || q.contains("date of birth") || q.contains("phone") || q.contains("email") || q.contains("father") || q.contains("mother");
    }

    private boolean containsSensitive(String text) {
        return EMAIL.matcher(text).find() || PHONE.matcher(text).find() || DOB.matcher(text).find()
                || text.toLowerCase(Locale.ROOT).contains("father") || text.toLowerCase(Locale.ROOT).contains("mother");
    }

    private boolean isSupportedByQuotes(String answer, List<CitationDto> citations) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        String quoteCorpus = citations.stream().map(c -> c.supportSpan() == null ? "" : c.supportSpan())
                .collect(Collectors.joining(" ")).toLowerCase(Locale.ROOT);
        Set<String> quoteTokens = new HashSet<>(tokenize(quoteCorpus));
        for (String token : tokenize(answer.replaceAll("\\[\\d+\\]", " "))) {
            if (!quoteTokens.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private int toInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("(?<=[.!?])\\s+"))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .replaceAll("\\s+", " ")
                        .trim()
                        .split(" "))
                .stream()
                .filter(token -> token.length() > 1)
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.toList());
    }

    private int overlaps(List<String> left, List<String> right) {
        int count = 0;
        for (String token : left) {
            if (right.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private int joinedLength(List<String> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return String.join(" ", values).length();
    }
}
