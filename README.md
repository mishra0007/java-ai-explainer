# Java AI Explainer

A Spring Boot service that explains Java exceptions and stack traces using Google Gemini.

## Endpoints

- `POST /api/explain` — Returns structured JSON with explanation, root cause, and fix
- `GET /api/explain/stream` — Streams the response token by token via SSE

## Setup

1. Clone the repo
2. Copy `application.properties.example` to `application.properties`
3. Add your Gemini API key from [aistudio.google.com](https://aistudio.google.com)
4. Run with `mvn spring-boot:run`

## Example

\`\`\`bash
curl -X POST http://localhost:8080/api/explain \
  -H "Content-Type: text/plain" \
  -d "What is a NullPointerException in Java?"
\`\`\`
