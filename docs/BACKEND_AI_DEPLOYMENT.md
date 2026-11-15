# SOLO LEVELING — REALITY OS: Backend AI Proxy Deployment Guide

## Security Architecture Overview
- **Zero Client Keys:** The Android application NEVER bundles, stores, or transmits `OPENROUTER_API_KEY` anywhere in APK resources, `BuildConfig`, `.env`, Room, or DataStore.
- **Backend Proxy Boundary:** All cloud OpenRouter requests travel from `HttpBackendAiGateway` (Android) → your Supabase Edge Function (`ai-game-master`) → OpenRouter API.
- **Deterministic Offline Fallback:** Whenever cloud consent is disabled, backend URL is unset, offline, rate-limited (`429`), auth-rejected (`401`/`403`), or OpenRouter fails (`5xx`/timeout), the app automatically falls back to `PersonalDevelopmentAnalyst` and `AiProposalValidator` on-device without crashing or blocking the user.

## Deploying the Supabase Edge Function

1. Install and authenticate the Supabase CLI:
   ```bash
   supabase login
   supabase link --project-ref <YOUR_PROJECT_REF>
   ```

2. Configure server-side secrets (never commit keys to Git):
   ```bash
   supabase secrets set OPENROUTER_API_KEY="sk-or-v1-..."
   # Optional: enable custom model IDs beyond the curated allowlist
   supabase secrets set ALLOW_CUSTOM_OPENROUTER_MODELS="false"
   # Optional: set if you gate requests with a custom header/bearer token
   # supabase secrets set CLIENT_SHARED_TOKEN="<strong-random-token>"
   ```

3. Deploy the Edge Function:
   ```bash
   supabase functions deploy ai-game-master --no-verify-jwt
   ```
   *(If you require Supabase Auth JWT validation, omit `--no-verify-jwt` and pass a user JWT from your auth session.)*

4. Connect the Android App:
   - Open **AI System → Settings** inside the app.
   - Enter your Supabase project URL (`https://<YOUR_PROJECT_REF>.supabase.co`) or full function URL (`https://<YOUR_PROJECT_REF>.supabase.co/functions/v1/ai-game-master`).
   - Enable **Cloud AI Consent** and toggle which minimized telemetry modules (`Physical`, `Academic`, `Quests`) you permit the assistant to see.

## Proxy Contract
- **Endpoint:** `POST /functions/v1/ai-game-master`
- **Request JSON:**
  ```json
  {
    "model": "google/gemini-2.5-flash",
    "requestType": "CHAT | QUEST_PROPOSALS | DAILY_PLAN",
    "systemPrompt": "...",
    "contextSummary": "...",
    "recentTurns": [{"role": "user", "content": "..."}],
    "userPrompt": "...",
    "stream": true
  }
  ```
- **Responses:**
  - Streaming (`stream: true`, `CHAT`): `text/event-stream` OpenRouter SSE chunks.
  - Non-streaming: `{ "content": "...", "modelUsed": "...", "promptTokens": 0, "completionTokens": 0 }`
