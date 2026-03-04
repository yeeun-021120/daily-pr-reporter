package com.team.prautoreporter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ClaudeService {

    @Value("${CLAUDE_API_KEY}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String summarize(String text) {

        String url = "https://api.anthropic.com/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-sonnet-4-6");
        body.put("max_tokens", 1000);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", "다음 GitHub 활동을 한국어로 정리해줘:\n\n" + text);

        body.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        List<Map<String, Object>> content =
                (List<Map<String, Object>>) response.getBody().get("content");

        return content.get(0).get("text").toString();
    }
}