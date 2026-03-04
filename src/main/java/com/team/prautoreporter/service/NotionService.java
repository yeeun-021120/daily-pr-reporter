package com.team.prautoreporter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
public class NotionService {

    @Value("${NOTION_TOKEN}")
    private String notionToken;

    @Value("${NOTION_DB_ID}")
    private String databaseId;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 🔥 가장 최근 리포트 날짜 조회
     */
    public String getLastReportDate() {

        String url = "https://api.notion.com/v1/databases/" 
                + databaseId + "/query";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(notionToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Notion-Version", "2022-06-28");

        Map<String, Object> body = new HashMap<>();
        body.put("sorts", List.of(
                Map.of(
                        "property", "Date",
                        "direction", "descending"
                )
        ));
        body.put("page_size", 1);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, entity, Map.class);

        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null || 
            ((List<?>) responseBody.get("results")).isEmpty()) {

            // 🔥 최초 실행이면 오늘 기준 3일 전으로 설정
            return java.time.OffsetDateTime.now()
                    .minusDays(3)
                    .toString();
        }

        Map<String, Object> page =
                (Map<String, Object>) ((List<?>) responseBody.get("results")).get(0);

        Map<String, Object> properties =
                (Map<String, Object>) page.get("properties");

        Map<String, Object> dateProperty =
                (Map<String, Object>) properties.get("Date");

        if (dateProperty == null || dateProperty.get("date") == null) {
            return java.time.OffsetDateTime.now()
                    .minusDays(3)
                    .toString();
        }

        Map<String, Object> dateValue =
                (Map<String, Object>) dateProperty.get("date");

        return dateValue.get("start").toString();
    }

    /**
     * 🔥 Notion 업로드
     */
    public void uploadToNotion(String summaryContent, int prCount) {

        String url = "https://api.notion.com/v1/pages";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(notionToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Notion-Version", "2022-06-28");

        Map<String, Object> properties = new HashMap<>();

        properties.put("Name", Map.of(
                "title", List.of(
                        Map.of("text", Map.of("content", "GitHub 자동 리포트"))
                )
        ));

        properties.put("Date", Map.of(
                "date", Map.of(
                        "start", LocalDate.now().toString()
                )
        ));

        properties.put("PR Count", Map.of(
                "number", prCount
        ));

        properties.put("AI Summary", Map.of(
                "rich_text", List.of(
                        Map.of("text",
                                Map.of("content", summaryContent))
                )
        ));

        Map<String, Object> body = new HashMap<>();
        body.put("parent", Map.of("database_id", databaseId));
        body.put("properties", properties);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, entity, String.class);

        System.out.println("===== Notion 업로드 완료 =====");
    }
}