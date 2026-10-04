# GenAI and Java: A One-Year Retrospective

Demos from my GeeCON 2026 talk, **"GenAI and Java: A One-Year Retrospective"**. The examples
use GeeCON's own agenda as the domain. They cover the main Java GenAI frameworks
(LangChain4j, Spring AI, Embabel), tool calling, guardrails, MCP, multi-agent
communication over A2A, and observability with OpenTelemetry.

📊 **Slides:** [GenAI and Java: A One-Year Retrospective (GeeCON 2026)](https://speakerdeck.com/piotrlaskawiec/genai-and-java-a-one-year-retrospective-geecon-2026)

## Modules

| Module | What it shows |
| --- | --- |
| `geecon-agenda-extractor` | Small library that fetches the GeeCON agenda. The other modules use it as their data source |
| `geecon-langchain4j-demo` | LangChain4j AI Services: structured output, tools, chat memory, input guardrail |
| `geecon-springai-demo` | Spring AI `ChatClient`: prompt templates, structured output, tools, chat memory, an advisor as a guardrail |
| `geecon-agenda-mcp-app` | Spring AI MCP server (streamable HTTP) with an MCP App UI for the schedule |
| `geecon-a2a-coordinator` | LangChain4j agentic coordinator (A2A client) with layered guardrails, on port `:8080` |
| `geecon-a2a-agenda-agent` | Spring AI agent (A2A server) that uses the GeeCON MCP server, on port `:8081` |
| `geecon-a2a-scoring-agent` | Embabel agent (A2A server) that scores talks in parallel, on port `:8082` |

## Requirements

- Java 25
- Docker (for the Grafana LGTM observability stack)
- An OpenAI API key

```bash
export OPENAI_API_KEY=sk-...
```

## Running

### Single-framework demos

```bash
# LangChain4j: run LangChain4jDemo.main() from your IDE
./gradlew :geecon-springai-demo:bootRun
```

### Multi-agent A2A demo

```bash
docker compose up -d                          # Grafana LGTM: UI at http://localhost:3000, OTLP at :4317/:4318

./gradlew :geecon-a2a-scoring-agent:bootRun   # :8082
./gradlew :geecon-a2a-agenda-agent:bootRun    # :8081
./gradlew :geecon-a2a-coordinator:bootRun     # :8080

curl -s -X POST http://localhost:8080/chat \
  -H 'content-type: application/json' \
  -d '{"preferences":"platform engineering, Java performance, observability"}' | jq .
```

Each `bootRun` attaches the OpenTelemetry Java agent, so one request produces a single
trace across all three services. Grafana (http://localhost:3000) has three provisioned
dashboards for the demo: **Tokens**, **Guardrails** and **Performance**.

The agenda agent connects to a public GeeCON MCP server by default. To use a local
`geecon-agenda-mcp-app` instead, set `GEECON_MCP_URL`.

## Disclaimer

This is conference demo code, not production code. Some framework versions are milestones
or early releases (for example, Spring AI 2.0 milestones).

## License

[MIT](LICENSE)
