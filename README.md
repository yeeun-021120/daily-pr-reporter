# PR Auto Reporter

GitHub 활동(PR, 커밋)을 자동으로 수집하고, Claude AI로 한국어 요약 후 Notion에 업로드하는 자동화 리포트 시스템입니다.

## 동작 방식

```
GitHub API → GitHubService
                 ↓
          ClaudeService (한국어 요약)
                 ↓
          NotionService (Notion DB 업로드)
```

매일 오전 9시(KST)에 자동 실행됩니다.
마지막 리포트 이후의 PR과 커밋만 수집하여 중복 없이 기록합니다.

## 기술 스택

- Java 17
- Spring Boot 4.0.3
- Claude API (`claude-sonnet-4-6`)
- GitHub REST API
- Notion API
- GitHub Actions

## 프로젝트 구조

```
src/main/java/com/team/prautoreporter/
├── scheduler/
│   └── ReportScheduler.java     # 매일 오전 9시 실행 스케줄러
└── service/
    ├── GitHubService.java        # PR 및 커밋 수집
    ├── ClaudeService.java        # Claude AI 한국어 요약
    └── NotionService.java        # Notion DB 조회 및 업로드
```

## 환경 변수 설정

`application.properties` 또는 GitHub Actions Secrets에 아래 값을 설정하세요.

| 변수명 | 설명 |
|---|---|
| `GITHUB_TOKEN` | GitHub Personal Access Token |
| `CLAUDE_API_KEY` | Anthropic Claude API Key |
| `NOTION_TOKEN` | Notion Integration Token |
| `NOTION_DB_ID` | 리포트를 저장할 Notion 데이터베이스 ID |

## Notion 데이터베이스 구조

Notion DB에 다음 속성이 필요합니다.

| 속성명 | 타입 |
|---|---|
| `Name` | 제목 (title) |
| `Date` | 날짜 (date) |
| `PR Count` | 숫자 (number) |
| `AI Summary` | 텍스트 (rich_text) |

## 로컬 실행

```bash
# 환경 변수를 직접 설정하거나 application.properties에 추가 후 실행
./mvnw spring-boot:run
```

> `spring.main.web-application-type=none` 설정으로 웹 서버 없이 스케줄러만 실행됩니다.

## GitHub Actions 자동 실행

`.github/workflows/daily-report.yml`에 정의된 워크플로우가 매일 UTC 00:00 (KST 09:00)에 실행됩니다.
`workflow_dispatch`로 수동 실행도 가능합니다.

GitHub 저장소 Settings → Secrets and variables → Actions에서 위 환경 변수를 등록하세요.

## 대상 저장소

현재 `JSL-26th-Ariana/Working-Title-DeMaSu` 저장소의 활동을 수집합니다.
변경하려면 `GitHubService.java`의 `owner`, `repo` 필드를 수정하세요.
