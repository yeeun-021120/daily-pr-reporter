import os
import requests
from datetime import datetime, timedelta

# ==============================
# 🔐 환경 변수
# ==============================
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
CLAUDE_API_KEY = os.getenv("CLAUDE_API_KEY")
NOTION_TOKEN = os.getenv("NOTION_TOKEN")
NOTION_DB_ID = os.getenv("NOTION_DB_ID")

OWNER = "JSL-26th-Ariana"
REPO = "Working-Title-DeMaSu"

# ==============================
# 📌 GitHub 활동 수집
# ==============================
def get_recent_activity():
    headers = {"Authorization": f"Bearer {GITHUB_TOKEN}"}

    yesterday = datetime.utcnow() - timedelta(days=1)
    since = yesterday.isoformat() + "Z"

    # Pull Requests
    pr_url = f"https://api.github.com/repos/{OWNER}/{REPO}/pulls?state=all&per_page=20"
    pr_res = requests.get(pr_url, headers=headers)

    if pr_res.status_code != 200:
        print(f"⚠️ GitHub PR 조회 실패 (HTTP {pr_res.status_code}): {pr_res.text}")
        pr_data = []
    else:
        pr_data = pr_res.json()

    activities = []

    for pr in pr_data:
        if pr.get("updated_at", "") > since:
            activities.append(
                f"PR: {pr['title']} (작성자: {pr['user']['login']})"
            )

    # Commits
    commit_url = f"https://api.github.com/repos/{OWNER}/{REPO}/commits?since={since}"
    commit_res = requests.get(commit_url, headers=headers)

    if commit_res.status_code != 200:
        print(f"⚠️ GitHub Commit 조회 실패 (HTTP {commit_res.status_code}): {commit_res.text}")
        commit_data = []
    else:
        commit_data = commit_res.json()

    for commit in commit_data:
        activities.append(
            f"Commit: {commit['commit']['message']} (작성자: {commit['commit']['author']['name']})"
        )

    return activities


# ==============================
# 🤖 Claude 요약
# ==============================
def summarize_with_claude(text):

    url = "https://api.anthropic.com/v1/messages"

    headers = {
        "x-api-key": CLAUDE_API_KEY,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json"
    }

    body = {
        "model": "claude-sonnet-4-5-20250929",
        "max_tokens": 1000,
        "messages": [
            {
                "role": "user",
                "content": f"다음 GitHub 활동을 요약해줘:\n\n{text}"
            }
        ]
    }

    response = requests.post(url, headers=headers, json=body)

    # HTTP 상태 코드 먼저 체크
    if response.status_code != 200:
        raise Exception(f"Claude API 실패 (HTTP {response.status_code}): {response.text}")

    try:
        data = response.json()
    except Exception:
        raise Exception(f"Claude 응답 파싱 실패: {response.text}")

    print("🔥 Claude Raw Response:", data)

    # content 키 존재 여부 체크
    if "content" not in data or not data["content"]:
        raise Exception(f"Claude 응답에 content 없음: {data}")

    return data["content"][0]["text"]


# ==============================
# 📝 Notion 업로드
# ==============================
def upload_to_notion(summary, pr_count):

    url = "https://api.notion.com/v1/pages"

    headers = {
        "Authorization": f"Bearer {NOTION_TOKEN}",
        "Notion-Version": "2022-06-28",
        "Content-Type": "application/json"
    }

    today = datetime.now().strftime("%Y-%m-%d")

    body = {
        "parent": {"database_id": NOTION_DB_ID},
        "properties": {
            "Name": {
                "title": [
                    {
                        "text": {
                            "content": f"GitHub 자동 리포트 - {today}"
                        }
                    }
                ]
            },
            "Date": {
                "date": {
                    "start": today
                }
            },
            "PR Count": {
                "number": pr_count
            }
        },
        "children": [
            {
                "object": "block",
                "type": "paragraph",
                "paragraph": {
                    "rich_text": [
                        {
                            "type": "text",
                            "text": {
                                "content": summary[:2000]
                            }
                        }
                    ]
                }
            }
        ]
    }

    response = requests.post(url, headers=headers, json=body)

    if response.status_code != 200:
        raise Exception(f"Notion 업로드 실패 (HTTP {response.status_code}): {response.text}")

    print("✅ Notion 업로드 완료")


# ==============================
# 🚀 실행부
# ==============================
if __name__ == "__main__":

    print("🚀 Daily Report Start")

    # 환경 변수 체크
    missing = []
    if not GITHUB_TOKEN:
        missing.append("GITHUB_TOKEN")
    if not CLAUDE_API_KEY:
        missing.append("CLAUDE_API_KEY")
    if not NOTION_TOKEN:
        missing.append("NOTION_TOKEN")
    if not NOTION_DB_ID:
        missing.append("NOTION_DB_ID")

    if missing:
        print(f"❌ 환경 변수 누락: {', '.join(missing)}")
        exit(1)

    activities = get_recent_activity()

    if not activities:
        print("오늘 변경 사항 없음")
        exit(0)

    joined_text = "\n".join(activities)
    print(f"📋 수집된 활동 {len(activities)}건")

    summary = summarize_with_claude(joined_text)
    print(f"📝 요약 완료: {summary[:100]}...")

    pr_count = sum(1 for a in activities if a.startswith("PR:"))

    upload_to_notion(summary, pr_count)

    print("🎉 Daily Report Completed")