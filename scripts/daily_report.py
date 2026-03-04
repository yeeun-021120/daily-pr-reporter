import os
import requests
from datetime import datetime, timedelta

GITHUB_TOKEN = os.getenv("GITHUB_TOKEN")
CLAUDE_KEY = os.getenv("CLAUDE_API_KEY")
NOTION_TOKEN = os.getenv("NOTION_TOKEN")
NOTION_DB_ID = os.getenv("NOTION_DB_ID")

OWNER = "JSL-26th-Ariana"
REPO = "Working-Title-DeMaSu"

def get_github_activity():
    since = (datetime.utcnow() - timedelta(days=1)).isoformat() + "Z"

    headers = {
        "Authorization": f"Bearer {GITHUB_TOKEN}"
    }

    pr_url = f"https://api.github.com/repos/{OWNER}/{REPO}/pulls?state=closed&sort=updated&direction=desc"
    commit_url = f"https://api.github.com/repos/{OWNER}/{REPO}/commits?since={since}"

    prs = requests.get(pr_url, headers=headers).json()
    commits = requests.get(commit_url, headers=headers).json()

    result = []

    for pr in prs:
        if pr.get("updated_at", "") > since:
            result.append(f"PR: {pr['title']} (작성자: {pr['user']['login']})")

    for c in commits:
        msg = c["commit"]["message"]
        author = c["commit"]["author"]["name"]
        result.append(f"Commit: {msg} (작성자: {author})")

    return result


def summarize_with_claude(text):
    url = "https://api.anthropic.com/v1/messages"

    headers = {
        "x-api-key": CLAUDE_KEY,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json"
    }

    body = {
        "model": "claude-3-5-sonnet-latest",
        "max_tokens": 500,
        "messages": [
            {"role": "user", "content": f"다음 GitHub 활동을 한국어로 요약해줘:\n{text}"}
        ]
    }

    res = requests.post(url, headers=headers, json=body)
    data = res.json()

    print("Claude raw response:", data)

    if "content" not in data:   
        raise Exception(f"Claude API Error: {data}")

    print("Claude raw response:", data)

if "content" not in data:
    raise Exception(f"Claude API Error: {data}")

return data["content"][0]["text"]


def upload_to_notion(summary, pr_count):
    url = "https://api.notion.com/v1/pages"

    headers = {
        "Authorization": f"Bearer {NOTION_TOKEN}",
        "Content-Type": "application/json",
        "Notion-Version": "2022-06-28"
    }

    body = {
        "parent": {"database_id": NOTION_DB_ID},
        "properties": {
            "Name": {
                "title": [
                    {"text": {"content": "GitHub 자동 리포트 (Actions)"}}
                ]
            },
            "Date": {
                "date": {"start": datetime.utcnow().date().isoformat()}
            },
            "PR Count": {
                "number": pr_count
            }
        }
    }

    requests.post(url, headers=headers, json=body)


if __name__ == "__main__":
    activities = get_github_activity()

    if not activities:
        print("변경사항 없음")
        exit()

    pr_count = len([a for a in activities if a.startswith("PR:")])

    summary = summarize_with_claude("\n".join(activities))

    upload_to_notion(summary, pr_count)

    print("Daily Report 완료")