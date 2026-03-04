package com.companyname.ragassistant.service;

import com.companyname.ragassistant.util.HashEmbedding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EmbeddingService {

    private final boolean enabled;
    private final String provider;
    private final int dim;
    private final String ollamaBaseUrl;
    private final String ollamaModel;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmbeddingService(@Value("${app.embeddings.enable:true}") boolean enabled,
                            @Value("${app.embeddings.provider:hash}") String provider,
                            @Value("${app.embeddings.dim:256}") int dim,
                            @Value("${app.embeddings.ollama.baseUrl:http://localhost:11434}") String ollamaBaseUrl,
                            @Value("${app.embeddings.ollama.model:nomic-embed-text}") String ollamaModel,
                            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.provider = provider;
        this.dim = dim;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaModel = ollamaModel;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    public float[] embed(String text) {
        if (!enabled) {
            return HashEmbedding.embed(text, dim);
        }
        if ("ollama".equalsIgnoreCase(provider)) {
            try {
                return embedWithOllama(text);
            } catch (Exception ignored) {
                return HashEmbedding.embed(text, dim);
            }
        }
        return HashEmbedding.embed(text, dim);
    }

    public String toJson(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize embedding", e);
        }
    }

    public float[] fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new float[0];
        }
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception e) {
            return new float[0];
        }
    }

    private float[] embedWithOllama(String text) throws Exception {
        String body = objectMapper.writeValueAsString(new OllamaEmbeddingRequest(ollamaModel, text));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaBaseUrl + "/api/embeddings"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Ollama embedding call failed: " + response.statusCode());
        }

        JsonNode node = objectMapper.readTree(response.body()).path("embedding");
        if (!node.isArray() || node.isEmpty()) {
            throw new IllegalStateException("Invalid embedding payload");
        }

        float[] vec = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            vec[i] = (float) node.get(i).asDouble(0d);
        }
        return vec;
    }

    private record OllamaEmbeddingRequest(String model, String prompt) {
    }
}
