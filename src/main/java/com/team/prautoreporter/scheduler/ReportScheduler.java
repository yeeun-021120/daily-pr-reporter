package com.team.prautoreporter.scheduler;

import com.team.prautoreporter.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportScheduler {

    private final GitHubService gitHubService;
    private final ClaudeService claudeService;
    private final NotionService notionService;

    @Scheduled(cron = "0 0 9 * * *")
    
    public void generateDailyReport() {

        System.out.println("===== 오전 9시 자동 리포트 시작 =====");

        // 🔥 마지막 리포트 날짜 조회
        String lastDate = notionService.getLastReportDate();

        // 🔥 변경된 활동만 조회
        List<String> activities =
                gitHubService.getActivitiesSince(lastDate);

        if (activities.isEmpty()) {
            System.out.println("변경 사항 없음");
            return;
        }

        int prCount = (int) activities.stream()
                .filter(a -> a.startsWith("PR:"))
                .count();

        String summary =
                claudeService.summarize(String.join("\n", activities));

        notionService.uploadToNotion(summary, prCount);

        System.out.println("===== 자동 리포트 완료 =====");
    }
}