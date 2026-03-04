package com.team.prautoreporter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GitHubService {

    @Value("${GITHUB_TOKEN}")
    private String githubToken;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String owner = "JSL-26th-Ariana";
    private final String repo = "Working-Title-DeMaSu";

    /**
     * 🔥 마지막 리포트 이후 변경된 활동 조회
     */
    public List<String> getActivitiesSince(String sinceDate) {

        List<String> result = new ArrayList<>();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 🔹 PR 조회
        String prUrl = "https://api.github.com/repos/"
                + owner + "/" + repo
                + "/pulls?state=closed&sort=updated&direction=desc&per_page=30";

        ResponseEntity<List> prResponse =
                restTemplate.exchange(prUrl, HttpMethod.GET, entity, List.class);

        List<Map<String, Object>> pulls = prResponse.getBody();

        if (pulls != null) {
            for (Map<String, Object> pr : pulls) {

                String updatedAt = pr.get("updated_at").toString();

                // sinceDate 이후 것만 필터
                if (updatedAt.compareTo(sinceDate) > 0) {

                    String title = pr.get("title").toString();
                    String user = ((Map<String, Object>) pr.get("user"))
                            .get("login").toString();

                    result.add("PR: " + title + " (작성자: " + user + ")");
                }
            }
        }

        // 🔹 Commit 조회 (since 파라미터 사용)
        String commitUrl = "https://api.github.com/repos/"
                + owner + "/" + repo
                + "/commits?since=" + sinceDate;

        ResponseEntity<List> commitResponse =
                restTemplate.exchange(commitUrl, HttpMethod.GET, entity, List.class);

        List<Map<String, Object>> commits = commitResponse.getBody();

        if (commits != null) {
            for (Map<String, Object> commitObj : commits) {

                Map<String, Object> commit =
                        (Map<String, Object>) commitObj.get("commit");

                String message = commit.get("message").toString();
                String author =
                        ((Map<String, Object>) commit.get("author"))
                                .get("name").toString();

                result.add("Commit: " + message + " (작성자: " + author + ")");
            }
        }

        return result;
    }
}