#!/usr/bin/env bash
set -euo pipefail

# Builds a Slack Block Kit payload for job failure notifications.
# Exports SLACK_FAILURE_PAYLOAD to BASH_ENV for use by the slack orb.
#
# Required env vars: CIRCLE_JOB, CIRCLE_BUILD_NUM, CIRCLE_BUILD_URL,
#   CIRCLE_WORKFLOW_ID, CIRCLE_BRANCH, CIRCLE_PROJECT_REPONAME
# Optional env vars: CIRCLE_PULL_REQUEST, CIRCLE_USERNAME,
#   CIRCLE_PROJECT_USERNAME, GITHUB_TOKEN

# Fetch workflow name from CircleCI API (no auth required)
WORKFLOW_NAME=$(curl -sf \
  "https://circleci.com/api/v2/workflow/${CIRCLE_WORKFLOW_ID}" \
  | jq -r '.name // empty' || true)
WORKFLOW_URL="https://app.circleci.com/pipelines/workflows/${CIRCLE_WORKFLOW_ID}"

# Fetch PR title if available
PR_TEXT=""
if [ -n "${CIRCLE_PULL_REQUEST:-}" ] && [ -n "${GITHUB_TOKEN:-}" ]; then
  PR_NUMBER="${CIRCLE_PULL_REQUEST##*/}"
  PR_TITLE=$(curl -sf \
    -H "Accept: application/vnd.github+json" \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    "https://api.github.com/repos/${CIRCLE_PROJECT_USERNAME:-}/${CIRCLE_PROJECT_REPONAME}/pulls/${PR_NUMBER}" \
    | jq -r '.title // empty' || true)
  if [ -n "$PR_TITLE" ]; then
    PR_TEXT="<${CIRCLE_PULL_REQUEST}|#${PR_NUMBER}: ${PR_TITLE}>"
  else
    PR_TEXT="<${CIRCLE_PULL_REQUEST}|#${PR_NUMBER}>"
  fi
fi

# Build payload safely with jq --arg for all dynamic values
PAYLOAD=$(jq -n \
  --arg job "${CIRCLE_JOB}/${CIRCLE_BUILD_NUM}" \
  --arg job_url "$CIRCLE_BUILD_URL" \
  --arg workflow "${WORKFLOW_NAME}" \
  --arg workflow_url "$WORKFLOW_URL" \
  --arg pr "$PR_TEXT" \
  --arg author "${CIRCLE_USERNAME:-}" \
  --arg branch "$CIRCLE_BRANCH" \
  --arg project "$CIRCLE_PROJECT_REPONAME" \
  '{
    "blocks": [
      {"type":"header","text":{"type":"plain_text","text":"🚨 Job Failed","emoji":true}},
      {"type":"section","fields":(
        [{"type":"mrkdwn","text":("*Job/Build:*\n<" + $job_url + "|" + $job + ">")}]
        + (if $workflow != "" then [{"type":"mrkdwn","text":("*Workflow:*\n<" + $workflow_url + "|" + $workflow + ">")}] else [] end)
        + (if $pr != "" then [{"type":"mrkdwn","text":("*PR:*\n" + $pr)}] else [] end)
        + (if $author != "" then [{"type":"mrkdwn","text":("*Author:*\n" + $author)}] else [] end)
        + [{"type":"mrkdwn","text":("*Branch:*\n" + $branch)}]
        + [{"type":"mrkdwn","text":("*Project:*\n" + $project)}]
      )}
    ]
  }')

echo "export SLACK_FAILURE_PAYLOAD='$(echo "$PAYLOAD" | sed "s/'/'\\\\''/g")'" >> "$BASH_ENV"
